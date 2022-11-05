package tech.quantit.northstar.gateway.playback.ticker;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.TickType;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 三角函数TICK仿真算法
 * 先确定开高低收对应的三角函数值，其中最高价为1，最低价为-1
 * @author KevinHuangwl
 *
 */
public class TrigonometricTickSimulation implements TickSimulationAlgorithm {
	
	private IContractManager contractMgr;
	
	private PlaybackPrecision precision;
	
	private int totalSize;
	
	private String gatewayId;
	
	public TrigonometricTickSimulation(String gatewayId, PlaybackPrecision precision, IContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.precision = precision;
		this.contractMgr = contractMgr;
		this.totalSize = switch (precision) {
		case EXTREME -> 1;
		case LOW -> 4;
		case MEDIUM -> 30;	// 中精度一共30个TICK
		case HIGH -> 120;	// 高精度一共120个TICK
		default -> throw new IllegalArgumentException("Unexpected value: " + precision);
		};
	}
	
	@Override
	public List<TickField> generateFrom(BarField bar) {
		ContractField contract = contractMgr.getContract(bar.getUnifiedSymbol());
		double priceTick = contract.getPriceTick();
		boolean isUp = bar.getClosePrice() > bar.getOpenPrice();  //是否为阳线
		int numOfPriceTickFromHighToLow = (int) Math.round((bar.getHighPrice() - bar.getLowPrice()) / priceTick);	// 最高最低价之间一共有多少个最小变动价位
		int numOfPriceTickFromOpenToLow = (int) Math.round((bar.getOpenPrice() - bar.getLowPrice()) / priceTick);
		int numOfPriceTickFromCloseToLow = (int) Math.round((bar.getClosePrice() - bar.getLowPrice()) / priceTick);
		if(numOfPriceTickFromHighToLow == 0 || precision == PlaybackPrecision.EXTREME) {
			return List.of(TickField.newBuilder()
					.setUnifiedSymbol(bar.getUnifiedSymbol())
					.setPreClosePrice(bar.getPreClosePrice())
					.setPreOpenInterest(bar.getPreOpenInterest())
					.setPreSettlePrice(bar.getPreSettlePrice())
					.setTradingDay(bar.getTradingDay())
					.setLastPrice(bar.getClosePrice())
					.setStatus(TickType.NORMAL_TICK.getCode())
					.setActionDay(bar.getActionDay())
					.setActionTime(LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER).minusMinutes(1).format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
					.setActionTimestamp(bar.getActionTimestamp() - 60000)
					.addAllAskPrice(List.of(bar.getClosePrice() + priceTick, 0D, 0D, 0D, 0D)) // 仅模拟卖一价
					.addAllBidPrice(List.of(bar.getClosePrice() - priceTick, 0D, 0D, 0D, 0D)) // 仅模拟买一价
					.setGatewayId(gatewayId)
					.setHighPrice(bar.getHighPrice())	
					.setLowPrice(bar.getLowPrice())		
					.setLowerLimit(0)
					.setUpperLimit(Integer.MAX_VALUE)
					.setVolumeDelta(bar.getVolumeDelta())
					.setVolume(bar.getVolume())
					.setOpenInterestDelta(bar.getOpenInterestDelta())
					.setOpenInterest(bar.getOpenInterest())
					.setTurnoverDelta(bar.getTurnoverDelta())
					.setTurnover(bar.getTurnover())
					.setNumTradesDelta(bar.getNumTradesDelta())
					.setNumTrades(bar.getNumTrades())
					.build());
		}
		double valuePerPriceTick = 2.0 / numOfPriceTickFromHighToLow;	// 每个价位在三角函数y坐标轴 [-1,1] 占据的比例 
		double offset = isUp ? 0 : Math.PI * 2;
		double highArcSinVal = Math.PI / 2;
		double lowArcSinVal = - (Math.PI / 2) + offset; 
		double openArcSinValTemp = Math.asin(-1 + numOfPriceTickFromOpenToLow * valuePerPriceTick);
		double openArcSinVal = isUp ? lowArcSinVal - Math.abs(openArcSinValTemp - lowArcSinVal) : openArcSinValTemp;
		double closeArcSinValTemp = Math.asin(-1 + numOfPriceTickFromCloseToLow * valuePerPriceTick);
		double closeArcSinVal = isUp ? highArcSinVal + Math.abs(highArcSinVal - closeArcSinValTemp) : closeArcSinValTemp + offset;
		
		int[] sectionLens = new int[3];
		sectionLens[0] = isUp ? numOfPriceTickFromOpenToLow : numOfPriceTickFromHighToLow - numOfPriceTickFromOpenToLow;
		sectionLens[1] = numOfPriceTickFromHighToLow;
		sectionLens[2] = isUp ? numOfPriceTickFromHighToLow - numOfPriceTickFromCloseToLow : numOfPriceTickFromCloseToLow;
		List<TickField> ticks = new ArrayList<>(totalSize);
		List<Double> ohlc = insertVals(List.of(openArcSinVal, closeArcSinVal, highArcSinVal, lowArcSinVal), sectionLens) ;
		List<Double> prices = ohlc.stream()
				.mapToDouble(Double::doubleValue)
				.map(Math::sin)
				.map(val -> Math.round((val + 1) / valuePerPriceTick) * priceTick + bar.getLowPrice())
				.mapToObj(Double::valueOf)
				.toList();
		int timeFrame = 60000 / totalSize;
		long tickVolDelta = bar.getVolume() / totalSize;
		double tickOpenInterestDelta = bar.getOpenInterestDelta() / totalSize;
		double tickTurnoverDelta = bar.getTurnover() / totalSize;
		long tickNumTradesDelta = bar.getNumTrades() / totalSize;
		
		for(int i=0; i<totalSize; i++) {
			ticks.add(TickField.newBuilder()
					.setUnifiedSymbol(bar.getUnifiedSymbol())
					.setPreClosePrice(bar.getPreClosePrice())
					.setPreOpenInterest(bar.getPreOpenInterest())
					.setPreSettlePrice(bar.getPreSettlePrice())
					.setTradingDay(bar.getTradingDay())
					.setLastPrice(prices.get(i))
					.setStatus(TickType.NORMAL_TICK.getCode())
					.setActionDay(bar.getActionDay())
					.setActionTime(LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER).minusSeconds(60 - i/2).format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
					.setActionTimestamp(bar.getActionTimestamp() - (totalSize - i) * timeFrame)
					.addAllAskPrice(List.of(prices.get(i) + priceTick, 0D, 0D, 0D, 0D)) // 仅模拟卖一价
					.addAllBidPrice(List.of(prices.get(i) - priceTick, 0D, 0D, 0D, 0D)) // 仅模拟买一价
					.setGatewayId(gatewayId)
					.setHighPrice(bar.getHighPrice())	
					.setLowPrice(bar.getLowPrice())		
					.setLowerLimit(0)
					.setUpperLimit(Integer.MAX_VALUE)
					.setVolumeDelta(tickVolDelta)
					.setVolume(bar.getVolume())
					.setOpenInterestDelta(tickOpenInterestDelta)
					.setOpenInterest(bar.getOpenInterest())
					.setTurnoverDelta(tickTurnoverDelta)
					.setTurnover(bar.getTurnover())
					.setNumTradesDelta(tickNumTradesDelta)
					.setNumTrades(bar.getNumTrades())
					.build());
		}
		return ticks;
	}
	
	private List<Double> insertVals(List<Double> source, int[] sectionLen){
		double[] sourceArr = source.stream()
				.mapToDouble(Double::doubleValue)
				.sorted()
				.toArray();
		if(precision == PlaybackPrecision.LOW) {
			return DoubleStream.of(sourceArr)
					.mapToObj(Double::valueOf)
					.toList();
		}
		// 插值分为前中后三段
		int totalStep = IntStream.of(sectionLen).sum();
		int actualStep = totalSize - 4;
		double convertFactor = actualStep * 1.0 / totalStep;  
		sectionLen[0] *= convertFactor;
		sectionLen[2] *= convertFactor;
		sectionLen[1] = actualStep - sectionLen[0] - sectionLen[2];
		List<Double> resultList = new ArrayList<>(totalSize);
		for(int i=0; i<3; i++) {
			resultList.add(sourceArr[i]);
			double rangeLow = Math.min(sourceArr[i], sourceArr[i+1]);
			double rangeHigh = Math.max(sourceArr[i], sourceArr[i+1]);
			resultList.addAll(makeSectionValues(rangeLow, rangeHigh, sectionLen[i]));
		}
		resultList.add(sourceArr[3]);
		return resultList;
	}
	
	private Random rand = new Random();
	private List<Double> makeSectionValues(double rangeLow, double rangeHigh, int numOfValToInsert){
		List<Double> sectionValues = new ArrayList<>();
		for(int i=0; i<numOfValToInsert; i++) {
			sectionValues.add(rangeLow == rangeHigh ? rangeHigh : rand.nextDouble(rangeLow, rangeHigh));
		}
		return sectionValues.stream().sorted().toList();
	}
	
}
