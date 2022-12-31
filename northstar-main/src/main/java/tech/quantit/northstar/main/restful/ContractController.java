package tech.quantit.northstar.main.restful;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;

/**
 * 
 * @author KevinHuangwl
 *
 */
@RequestMapping("/northstar/contracts")
@RestController
public class ContractController {

	@Autowired
	IContractManager contractMgr; 
	
	@GetMapping
	public ResultBean<List<ContractSimpleInfo>> channelContracts(ChannelType channelType){
		return new ResultBean<>(contractMgr.getContracts(channelType).stream()
				.map(c -> ContractSimpleInfo.builder().name(c.name()).identifier(c.identifier().value()).build())
				.toList());
	}
	
	@GetMapping("/subscribed")
	public ResultBean<List<ContractSimpleInfo>> subscribedContracts(String gatewayId){
		return new ResultBean<>(contractMgr.getContracts(gatewayId).stream()
				.filter(Contract::hasSubscribed)
				.map(c -> ContractSimpleInfo.builder().name(c.name()).identifier(c.identifier().value()).build())
				.toList());
	}
}
