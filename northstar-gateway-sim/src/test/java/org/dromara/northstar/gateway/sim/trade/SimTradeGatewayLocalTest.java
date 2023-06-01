package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class SimTradeGatewayLocalTest {
	
	private SimTradeGatewayLocal gateway;
	
	private TestFieldFactory factory = new TestFieldFactory("test");
	
	@BeforeEach
	void prepare() {
		FastEventEngine feEngine = mock(FastEventEngine.class);
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("gatewayId")
				.bindedMktGatewayId("bindedGatewayId")
				.build();
		SimGatewayAccount simAccount = mock(SimGatewayAccount.class);
		gateway = new SimTradeGatewayLocal(feEngine, gd, simAccount);
	}

	@Test
	void testConnectAndDisConnect() {
		gateway.connect();
		assertThat(gateway.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);
		
		gateway.disconnect();
		assertThat(gateway.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
	}

	@Test
	void testSubmitOrder() {
		gateway.connect();
		gateway.submitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 0, 0));
	}

	@Test
	void testCancelOrder() {
		gateway.connect();
		gateway.cancelOrder(factory.makeCancelReq(factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 0, 0)));
	}

	@Test
	void testMoneyIO() {
		gateway.moneyIO(1);
		verify(gateway.account).onDeposit(1);
		gateway.moneyIO(-1);
		verify(gateway.account).onWithdraw(1);
	}

	@Test
	void testGetAuthErrorFlag() {
		assertThat(gateway.getAuthErrorFlag()).isFalse();
	}

}
