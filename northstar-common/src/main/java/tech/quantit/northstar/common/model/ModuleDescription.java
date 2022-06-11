package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.ModuleType;

/**
 * 模组配置信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDescription {

	/**
	 * 模组名称
	 */
	private String moduleName;
	/**
	 * 模组类型
	 */
	private ModuleType type;
	/**
	 * K线周期数
	 */
	private int numOfMinPerBar;
	/**
	 * 预热K线数
	 */
	private int numOfBarForPreparation;
	/**
	 * 平仓优化策略
	 */
	private ClosingPolicy closingPolicy;
	/**
	 * 模组账户配置信息 
	 */
	private List<ModuleAccountDescription> moduleAccountSettingsDescription;
	/**
	 * 策略配置信息
	 */
	private ComponentAndParamsPair strategySetting;
}