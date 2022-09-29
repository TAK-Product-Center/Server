package tak.server.plugins;

public class PluginResponse {

	String data;
	String contentType;
	
	public PluginResponse() {
		
	}
	
	public PluginResponse(String data, String contentType){
		this.data = data;
		this.contentType = contentType;
	}
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public String getContentType() {
		return this.contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	@Override
	public String toString() {
		return "PluginResponse [data=" + data + ", contentType=" + contentType + "]";
	}
}
