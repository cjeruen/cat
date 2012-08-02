package com.dianping.cat.notify.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dianping.cat.consumer.event.model.entity.EventReport;
import com.dianping.cat.consumer.event.model.entity.EventType;
import com.dianping.cat.consumer.problem.model.entity.ProblemReport;
import com.dianping.cat.consumer.transaction.model.entity.TransactionReport;
import com.dianping.cat.consumer.transaction.model.entity.TransactionType;
import com.dianping.cat.notify.job.ProblemStatistics;
import com.dianping.cat.notify.util.TimeUtil;

public class WeaklyReportCreater extends AbstractReportCreater {

	public boolean isNeedToCreate(long timestamp) {
		int dayOfWeak = TimeUtil.getDayOfWeak(timestamp);
		int hour = TimeUtil.getHourOfDay(timestamp);
		/* create report at 00:00:00 of every saturday */
		if (dayOfWeak != 7 || hour != 0) {
			return false;
		}
		long currentTime = System.currentTimeMillis();
		if (lastSuccessTime.get() == -1) {
			/* for first time */
			lastSuccessTime.set(currentTime);
			return true;
		}

		long timespan = currentTime - lastSuccessTime.get();
		if (timespan > TimeUtil.HOUR_MICROS) {
			/* next time */
			lastSuccessTime.set(currentTime);
			return true;
		}
		return false;
	}

	@Override
	protected TimeSpan getReportTimeSpan(long timespan) {
		long startMicros = timespan - TimeUtil.DAY_MICROS * 7;
		long endMicros = timespan - TimeUtil.DAY_MICROS;
		TimeSpan timeRange = new TimeSpan();
		timeRange.setStartMicros(startMicros);
		timeRange.setEndMicros(endMicros);
		timeRange.setTimeStamp(timespan);
		return timeRange;
	}

	@Override
	protected String renderTransactionReport(TimeSpan timeSpan,
			TransactionReport report) {
		com.dianping.cat.consumer.transaction.model.entity.Machine machine = report
				.findMachine(ReportConstants.ALL_IP);
		if (machine == null) {
			return null;
		}
		List<TransactionType> typeList = new ArrayList<TransactionType>(machine
				.getTypes().values());
		Collections.sort(typeList, new Comparator<TransactionType>() {
			@Override
			public int compare(TransactionType o1, TransactionType o2) {
				return (int) (o1.getAvg() - o2.getAvg());
			}
		});

		Map<String, Object> params = new HashMap<String, Object>();

		String startDay = TimeUtil.formatTime("yyyy-MM-dd",
				timeSpan.getStartMicros());
		String endDay = TimeUtil.formatTime("yyyy-MM-dd",
				timeSpan.getEndMicros());
		params.put("title", String.format(
				"Weekly Transaction Report [%s to %s]", startDay, endDay));

		params.put("typeList", typeList);
		String templatePath = m_config.getTemplates().get("transaction")
				.getPath();
		return m_render.fetchAll(templatePath, params);
	}

	@Override
	protected String renderEventReport(TimeSpan timeSpan, EventReport report) {
		com.dianping.cat.consumer.event.model.entity.Machine machine = report
				.findMachine(ReportConstants.ALL_IP);
		if (machine == null) {
			return null;
		}
		List<EventType> eventTypeList = new ArrayList<EventType>(machine
				.getTypes().values());
		Collections.sort(eventTypeList, new Comparator<EventType>() {
			@Override
			public int compare(EventType o1, EventType o2) {
				return (int) (o1.getTotalCount() - o2.getTotalCount());
			}
		});

		Map<String, Object> params = new HashMap<String, Object>(2);

		String startDay = TimeUtil.formatTime("yyyy-MM-dd",
				timeSpan.getStartMicros());
		String endDay = TimeUtil.formatTime("yyyy-MM-dd",
				timeSpan.getEndMicros());
		params.put("title", String.format("Weekly Event Report [%s to %s]",
				startDay, endDay));

		params.put("typeList", eventTypeList);
		String templatePath = m_config.getTemplates().get("event").getPath();
		return m_render.fetchAll(templatePath, params);
	}

	@Override
	protected String renterProblemReport(TimeSpan timeSpan, ProblemReport report) {
		ProblemStatistics problemStatistics = new ProblemStatistics();
		problemStatistics.setAllIp(true);
		problemStatistics.visitProblemReport(report);

		Map<String, Object> params = new HashMap<String, Object>(2);

		String startDay = TimeUtil.formatTime("yyyy-MM-dd",
				timeSpan.getStartMicros());
		String endDay = TimeUtil.formatTime("yyyy-MM-dd",
				timeSpan.getEndMicros());
		params.put("title", String.format("Weekly Problem Report [%s to %s]",
				startDay, endDay));

		params.put("problemStatistics", problemStatistics);
		String templatePath = m_config.getTemplates().get("problem").getPath();
		return m_render.fetchAll(templatePath, params);
	}
}
