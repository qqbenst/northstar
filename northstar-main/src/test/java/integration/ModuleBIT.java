package integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson.JSON;

import common.TestGatewayFactory;
import common.TestMongoUtils;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.main.NorthstarApplication;
import tech.xuanwu.northstar.strategy.common.model.ModulePosition;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
public class ModuleBIT {

	private String symbol;
	
	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@Before
	public void setUp() throws Exception {
		LocalDate date = LocalDate.now().plusDays(45);
		String year = date.getYear() % 100 + "";
		String month = String.format("%02d", date.getMonth().getValue());
		symbol = "ni" + year + month;
		
		session = new MockHttpSession();
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
		
		String json = JSON.toJSONString(TestGatewayFactory.makeTrdGateway("TG1", "TG2",  GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false));
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(json).session(session))
			.andExpect(status().isOk());
	}
	
	@After
	public void tearDown() throws InterruptedException {
		TestMongoUtils.clearDB();
	}
	
	@Test
	public void shouldSuccessfullyCreate() throws Exception {
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"actionInterval\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"秒\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"priceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]},{\"label\":\"超价\",\"name\":\"overprice\",\"order\":40,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"Tick\",\"options\":[]}]},\"enabled\":false,\"type\":\"CTA\"}";
		
		mockMvc.perform(post("/module").contentType(MediaType.APPLICATION_JSON_UTF8).content(demoStr).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyUpdate() throws Exception {
		shouldSuccessfullyCreate();
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"actionInterval\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"秒\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"priceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]},{\"label\":\"超价\",\"name\":\"overprice\",\"order\":40,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"Tick\",\"options\":[]}]},\"enabled\":true,\"type\":\"CTA\"}";
		
		mockMvc.perform(put("/module").contentType(MediaType.APPLICATION_JSON_UTF8).content(demoStr).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldFindModules() throws Exception {
		mockMvc.perform(get("/module").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyRemove() throws Exception {
		shouldSuccessfullyCreate();
		
		mockMvc.perform(delete("/module?name=TEST").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldCreateModulePosition() throws Exception {
		prepareGateway();
		System.out.println("等待gateway初始化");
		Thread.sleep(2000);
		shouldSuccessfullyCreate();
		ModulePosition position = ModulePosition.builder()
				.unifiedSymbol(symbol+"@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		
		mockMvc.perform(post("/module/TEST/position").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(position)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldUpdateModulePosition() throws Exception {
		prepareGateway();
		shouldSuccessfullyCreate();
		ModulePosition position = ModulePosition.builder()
				.unifiedSymbol(symbol+"@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		
		mockMvc.perform(put("/module/TEST/position").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(position)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldRemoveModulePosition() throws Exception {
		shouldCreateModulePosition();
		
		mockMvc.perform(delete("/module/TEST/position?unifiedSymbol="+symbol+"@SHFE@FUTURES&dir=PD_Long").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	private void prepareGateway() throws Exception {
		GatewayDescription gateway = TestGatewayFactory.makeMktGateway("TG5", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),false);
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(gateway)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		GatewayDescription gateway2 = TestGatewayFactory.makeTrdGateway("TG5Account", "TG5", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),true);
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(gateway2)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		Thread.sleep(1000);
	}
	
//	@Test
//	public void shouldSuccessfullyGetModuleCurrentPerformance() {
//		
//	}
//	
//	@Test
//	public void shouldSuccessfullyGetModuleHistoryPerformance() {}
//	
//	@Test
//	public void shouldSuccessfullyToggleModuleState() {}
//	
	
}