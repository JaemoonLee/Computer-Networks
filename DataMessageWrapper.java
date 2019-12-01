public class DataMessageWrapper {
	
	private DataMessage dataMsg;
	private String fromPeerID;
	
	public DataMessageWrapper() {
		dataMsg = new DataMessage();
		fromPeerID = null;
	}
	
    public void setFromPeerID(String fromPeerID) {
        this.fromPeerID = fromPeerID;
    }
    
	public String getFromPeerID() {
		return fromPeerID;
	}
	
	public void setDataMsg(DataMessage dataMsg) {
        this.dataMsg = dataMsg;
    }
	
    public DataMessage getDataMsg() {
		return dataMsg;
	}
}