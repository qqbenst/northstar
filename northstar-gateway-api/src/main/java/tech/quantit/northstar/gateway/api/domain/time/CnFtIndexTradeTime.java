package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.List;

/**
 * 国内金融期货二类品种交易时间（股指）
 * @author KevinHuangwl
 *
 */
public final class CnFtIndexTradeTime implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> getPeriodSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(9, 31), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 1), LocalTime.of(15, 00))
			);
	}

}
