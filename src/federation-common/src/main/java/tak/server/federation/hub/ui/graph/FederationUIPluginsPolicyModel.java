package tak.server.federation.hub.ui.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

import tak.server.federation.hub.ui.graph.FederationUIPluginsPolicyModel.PluginPolicyModel;

/**
 * Object used to hold front end UI's representation of a federation policy
 * graph.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FederationUIPluginsPolicyModel {

	private List<PluginPolicyModel> plugins = new ArrayList<>();

	public List<PluginPolicyModel> getPlugins() {
	    return plugins;
	}

	public void setPlugins(List<PluginPolicyModel> plugins) {
	    this.plugins = plugins;
	}
	
	@JsonIgnore
	public Map<String, PluginPolicyModel> getPluginsAsMap() {
		return plugins.stream()
				.collect(Collectors.toMap(PluginPolicyModel::getPluginName, Function.identity()));
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PluginPolicyModel {
		private String pluginName;
		private Set<String> sendTo;
		private Set<String> dontSend;
		private Set<String> receiveFrom;
		private Set<String> dontReceive;
		private boolean sendByDefault;
		private boolean receiveByDefault;
		private boolean disabled;
		public String getPluginName() {
			return pluginName;
		}
		public void setPluginName(String pluginName) {
			this.pluginName = pluginName;
		}
		public Set<String> getSendTo() {
			return sendTo;
		}
		public void setSendTo(Set<String> sendTo) {
			this.sendTo = sendTo;
		}
		public Set<String> getDontSend() {
			return dontSend;
		}
		public void setDontSend(Set<String> dontSend) {
			this.dontSend = dontSend;
		}
		public Set<String> getReceiveFrom() {
			return receiveFrom;
		}
		public void setReceiveFrom(Set<String> receiveFrom) {
			this.receiveFrom = receiveFrom;
		}
		public Set<String> getDontReceive() {
			return dontReceive;
		}
		public void setDontReceive(Set<String> dontReceive) {
			this.dontReceive = dontReceive;
		}
		public boolean isSendByDefault() {
			return sendByDefault;
		}
		public void setSendByDefault(boolean sendByDefault) {
			this.sendByDefault = sendByDefault;
		}
		public boolean isReceiveByDefault() {
			return receiveByDefault;
		}
		public void setReceiveByDefault(boolean receiveByDefault) {
			this.receiveByDefault = receiveByDefault;
		}
		public boolean isDisabled() {
			return disabled;
		}
		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}
		@Override
		public String toString() {
			return "PluginPolicyModel [pluginName=" + pluginName + ", sendTo=" + sendTo + ", dontSend=" + dontSend
					+ ", receiveFrom=" + receiveFrom + ", dontReceive=" + dontReceive + ", sendByDefault="
					+ sendByDefault + ", receiveByDefault=" + receiveByDefault + ", disabled=" + disabled + "]";
		}
	}
}