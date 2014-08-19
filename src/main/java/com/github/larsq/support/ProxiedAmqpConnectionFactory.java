package com.github.larsq.support;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * Created by lareri on 2014-08-18.
 */
public class ProxiedAmqpConnectionFactory extends ConnectionFactory {
	private final static Logger LOG = LoggerFactory.getLogger(ProxiedAmqpConnectionFactory.class);


	public static class ProxiedAmqpChannel implements Channel {
		private Channel channel;

		@Override
		public int getChannelNumber() {
			return channel.getChannelNumber();
		}

		@Override
		public Connection getConnection() {
			return channel.getConnection();
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}

		@Override
		public void close(int i, String s) throws IOException {
			channel.close(i, s);
		}

		@Override
		public boolean flowBlocked() {
			return channel.flowBlocked();
		}

		@Override
		public void abort() throws IOException {
			channel.abort();
		}

		@Override
		public void abort(int i, String s) throws IOException {
			channel.abort(i, s);
		}

		@Override
		public void addReturnListener(ReturnListener returnListener) {
			channel.addReturnListener(returnListener);
		}

		@Override
		public boolean removeReturnListener(ReturnListener returnListener) {
			return channel.removeReturnListener(returnListener);
		}

		@Override
		public void clearReturnListeners() {
			channel.clearReturnListeners();
		}

		@Override
		public void addFlowListener(FlowListener flowListener) {
			channel.addFlowListener(flowListener);
		}

		@Override
		public boolean removeFlowListener(FlowListener flowListener) {
			return channel.removeFlowListener(flowListener);
		}

		@Override
		public void clearFlowListeners() {
			channel.clearFlowListeners();
		}

		@Override
		public void addConfirmListener(ConfirmListener confirmListener) {
			channel.addConfirmListener(confirmListener);
		}

		@Override
		public boolean removeConfirmListener(ConfirmListener confirmListener) {
			return channel.removeConfirmListener(confirmListener);
		}

		@Override
		public void clearConfirmListeners() {
			channel.clearConfirmListeners();
		}

		@Override
		public Consumer getDefaultConsumer() {
			return channel.getDefaultConsumer();
		}

		@Override
		public void setDefaultConsumer(Consumer consumer) {
			channel.setDefaultConsumer(consumer);
		}

		@Override
		public void basicQos(int i, int i2, boolean b) throws IOException {
			channel.basicQos(i, i2, b);
		}

		@Override
		public void basicQos(int i, boolean b) throws IOException {

			channel.basicQos(i, b);
		}

		@Override
		public void basicQos(int i) throws IOException {
			channel.basicQos(i);
		}

		@Override
		public void basicPublish(String s, String s2, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
			channel.basicPublish(s, s2, basicProperties, bytes);
		}

		@Override
		public void basicPublish(String s, String s2, boolean b, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
			LOG.debug("basicPublish");

			channel.basicPublish(s, s2, b, basicProperties, bytes);
		}

		@Override
		public void basicPublish(String s, String s2, boolean b, boolean b2, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
			LOG.debug("basicPublish");

			channel.basicPublish(s, s2, b, b2, basicProperties, bytes);
		}

		@Override
		public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s2) throws IOException {
			return channel.exchangeDeclare(s, s2);
		}

		@Override
		public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s2, boolean b) throws IOException {
			return channel.exchangeDeclare(s, s2, b);
		}

		@Override
		public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s2, boolean b, boolean b2, Map<String, Object> map) throws IOException {
			return channel.exchangeDeclare(s, s2, b, b2, map);
		}

		@Override
		public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s2, boolean b, boolean b2, boolean b3, Map<String, Object> map) throws IOException {
			return channel.exchangeDeclare(s, s2, b, b2, b3, map);
		}

		@Override
		public AMQP.Exchange.DeclareOk exchangeDeclarePassive(String s) throws IOException {
			return channel.exchangeDeclarePassive(s);
		}

		@Override
		public AMQP.Exchange.DeleteOk exchangeDelete(String s, boolean b) throws IOException {
			return channel.exchangeDelete(s, b);
		}

		@Override
		public AMQP.Exchange.DeleteOk exchangeDelete(String s) throws IOException {
			return channel.exchangeDelete(s);
		}

		@Override
		public AMQP.Exchange.BindOk exchangeBind(String s, String s2, String s3) throws IOException {
			return channel.exchangeBind(s, s2, s3);
		}

		@Override
		public AMQP.Exchange.BindOk exchangeBind(String s, String s2, String s3, Map<String, Object> map) throws IOException {
			return channel.exchangeBind(s, s2, s3, map);
		}

		@Override
		public AMQP.Exchange.UnbindOk exchangeUnbind(String s, String s2, String s3) throws IOException {
			return channel.exchangeUnbind(s, s2, s3);
		}

		@Override
		public AMQP.Exchange.UnbindOk exchangeUnbind(String s, String s2, String s3, Map<String, Object> map) throws IOException {
			return channel.exchangeUnbind(s, s2, s3, map);
		}

		@Override
		public AMQP.Queue.DeclareOk queueDeclare() throws IOException {
			return channel.queueDeclare();
		}

		@Override
		public AMQP.Queue.DeclareOk queueDeclare(String s, boolean b, boolean b2, boolean b3, Map<String, Object> map) throws IOException {
			return channel.queueDeclare(s, b, b2, b3, map);
		}

		@Override
		public AMQP.Queue.DeclareOk queueDeclarePassive(String s) throws IOException {
			return channel.queueDeclarePassive(s);
		}

		@Override
		public AMQP.Queue.DeleteOk queueDelete(String s) throws IOException {
			return channel.queueDelete(s);
		}

		@Override
		public AMQP.Queue.DeleteOk queueDelete(String s, boolean b, boolean b2) throws IOException {
			return channel.queueDelete(s, b, b2);
		}

		@Override
		public AMQP.Queue.BindOk queueBind(String s, String s2, String s3) throws IOException {
			return channel.queueBind(s, s2, s3);
		}

		@Override
		public AMQP.Queue.BindOk queueBind(String s, String s2, String s3, Map<String, Object> map) throws IOException {
			return channel.queueBind(s, s2, s3, map);
		}

		@Override
		public AMQP.Queue.UnbindOk queueUnbind(String s, String s2, String s3) throws IOException {
			return channel.queueUnbind(s, s2, s3);
		}

		@Override
		public AMQP.Queue.UnbindOk queueUnbind(String s, String s2, String s3, Map<String, Object> map) throws IOException {
			return channel.queueUnbind(s, s2, s3, map);
		}

		@Override
		public AMQP.Queue.PurgeOk queuePurge(String s) throws IOException {
			return channel.queuePurge(s);
		}

		@Override
		public GetResponse basicGet(String s, boolean b) throws IOException {
			LOG.debug("basicGet");

			return channel.basicGet(s, b);
		}

		@Override
		public void basicAck(long l, boolean b) throws IOException {
			LOG.debug("basicAck");

			channel.basicAck(l, b);
		}

		@Override
		public void basicNack(long l, boolean b, boolean b2) throws IOException {
			LOG.debug("basicNack");

			channel.basicNack(l, b, b2);
		}

		@Override
		public void basicReject(long l, boolean b) throws IOException {
			LOG.debug("basicReject");

			channel.basicReject(l, b);
		}

		@Override
		public String basicConsume(String s, Consumer consumer) throws IOException {
			LOG.debug("basicConsume");

			return channel.basicConsume(s, consumer);
		}

		@Override
		public String basicConsume(String s, boolean b, Consumer consumer) throws IOException {
			LOG.debug("basicConsume");

			return channel.basicConsume(s, b, consumer);
		}

		@Override
		public String basicConsume(String s, boolean b, Map<String, Object> map, Consumer consumer) throws IOException {
			LOG.debug("basicConsume");

			return channel.basicConsume(s, b, map, consumer);
		}

		@Override
		public String basicConsume(String s, boolean b, String s2, Consumer consumer) throws IOException {
			LOG.debug("basicConsume");

			return channel.basicConsume(s, b, s2, consumer);
		}

		@Override
		public String basicConsume(String s, boolean b, String s2, boolean b2, boolean b3, Map<String, Object> map, Consumer consumer) throws IOException {
			LOG.debug("basicConsume");

			return channel.basicConsume(s, b, s2, b2, b3, map, consumer);
		}

		@Override
		public void basicCancel(String s) throws IOException {
			LOG.debug("basicCancel");

			channel.basicCancel(s);
		}

		@Override
		public AMQP.Basic.RecoverOk basicRecover() throws IOException {
			LOG.debug("basicRecover");

			return channel.basicRecover();
		}

		@Override
		public AMQP.Basic.RecoverOk basicRecover(boolean b) throws IOException {
			LOG.debug("basicRecover");

			return channel.basicRecover(b);
		}

		@Override
		@Deprecated
		public void basicRecoverAsync(boolean b) throws IOException {
			LOG.debug("basicAsync");

			channel.basicRecoverAsync(b);
		}

		@Override
		public AMQP.Tx.SelectOk txSelect() throws IOException {
			return channel.txSelect();
		}

		@Override
		public AMQP.Tx.CommitOk txCommit() throws IOException {
			return channel.txCommit();
		}

		@Override
		public AMQP.Tx.RollbackOk txRollback() throws IOException {
			return channel.txRollback();
		}

		@Override
		public AMQP.Confirm.SelectOk confirmSelect() throws IOException {
			return channel.confirmSelect();
		}

		@Override
		public long getNextPublishSeqNo() {
			return channel.getNextPublishSeqNo();
		}

		@Override
		public boolean waitForConfirms() throws InterruptedException {
			LOG.debug("waitForConfirms");

			return channel.waitForConfirms();
		}

		@Override
		public boolean waitForConfirms(long l) throws InterruptedException, TimeoutException {
			LOG.debug("waitForConfirms");

			return channel.waitForConfirms(l);
		}

		@Override
		public void waitForConfirmsOrDie() throws IOException, InterruptedException {
			channel.waitForConfirmsOrDie();
		}

		@Override
		public void waitForConfirmsOrDie(long l) throws IOException, InterruptedException, TimeoutException {
			channel.waitForConfirmsOrDie(l);
		}

		@Override
		public void asyncRpc(Method method) throws IOException {
			LOG.debug("asyncRpc");

			channel.asyncRpc(method);
		}

		@Override
		public Command rpc(Method method) throws IOException {
			LOG.debug("rpc");

			return channel.rpc(method);
		}

		@Override
		public void addShutdownListener(ShutdownListener shutdownListener) {
			channel.addShutdownListener(shutdownListener);
		}

		@Override
		public void removeShutdownListener(ShutdownListener shutdownListener) {
			channel.removeShutdownListener(shutdownListener);
		}

		@Override
		public ShutdownSignalException getCloseReason() {
			return channel.getCloseReason();
		}

		@Override
		public void notifyListeners() {
			channel.notifyListeners();
		}

		@Override
		public boolean isOpen() {
			return false;
		}
	}

	public static class ProxiedAmqpConnection implements Connection {
		private Connection connection;

		public ProxiedAmqpConnection(Connection connection) {
			this.connection = connection;
		}

		@Override
		public InetAddress getAddress() {
			return connection.getAddress();
		}

		@Override
		public int getPort() {
			return connection.getPort();
		}

		@Override
		public int getChannelMax() {
			return connection.getChannelMax();
		}

		@Override
		public int getFrameMax() {
			return connection.getFrameMax();
		}

		@Override
		public int getHeartbeat() {
			return connection.getHeartbeat();
		}

		@Override
		public Map<String, Object> getClientProperties() {
			return connection.getClientProperties();
		}

		@Override
		public Map<String, Object> getServerProperties() {
			return connection.getServerProperties();
		}

		@Override
		public Channel createChannel() throws IOException {
			return connection.createChannel();
		}

		@Override
		public Channel createChannel(int i) throws IOException {
			return connection.createChannel(i);
		}

		@Override
		public void close() throws IOException {
			connection.close();
		}

		@Override
		public void close(int i, String s) throws IOException {
			connection.close(i, s);
		}

		@Override
		public void close(int i) throws IOException {
			connection.close(i);
		}

		@Override
		public void close(int i, String s, int i2) throws IOException {
			connection.close(i, s, i2);
		}

		@Override
		public void abort() {
			connection.abort();
		}

		@Override
		public void abort(int i, String s) {
			connection.abort(i, s);
		}

		@Override
		public void abort(int i) {
			connection.abort(i);
		}

		@Override
		public void abort(int i, String s, int i2) {
			connection.abort(i, s, i2);
		}

		@Override
		public void addBlockedListener(BlockedListener blockedListener) {
			connection.addBlockedListener(blockedListener);
		}

		@Override
		public boolean removeBlockedListener(BlockedListener blockedListener) {
			return connection.removeBlockedListener(blockedListener);
		}

		@Override
		public void clearBlockedListeners() {
			connection.clearBlockedListeners();
		}

		@Override
		public ExceptionHandler getExceptionHandler() {
			return connection.getExceptionHandler();
		}

		@Override
		public void addShutdownListener(ShutdownListener shutdownListener) {
			connection.addShutdownListener(shutdownListener);
		}

		@Override
		public void removeShutdownListener(ShutdownListener shutdownListener) {
			connection.removeShutdownListener(shutdownListener);
		}

		@Override
		public ShutdownSignalException getCloseReason() {
			return connection.getCloseReason();
		}

		@Override
		public void notifyListeners() {
			connection.notifyListeners();
		}

		@Override
		public boolean isOpen() {
			return connection.isOpen();
		}
	}

	@Override
	public Connection newConnection(ExecutorService executor) throws IOException {
		return new ProxiedAmqpConnection(super.newConnection(executor));
	}

	@Override
	public Connection newConnection() throws IOException {
		return new ProxiedAmqpConnection(super.newConnection());
	}


	@Override
	public Connection newConnection(ExecutorService executor, Address[] addrs) throws IOException {
		return new ProxiedAmqpConnection(super.newConnection(executor, addrs));
	}

	@Override
	public Connection newConnection(Address[] addrs) throws IOException {
		return new ProxiedAmqpConnection(super.newConnection(addrs));
	}
}
