
public enum DataMessageType {

	DATA_MSG_CHOKE("0"),
	DATA_MSG_UNCHOKE("1"),
	DATA_MSG_INTERESTED("2"),
	DATA_MSG_NOTINTERESTED("3"),
	DATA_MSG_HAVE("4"),
	DATA_MSG_BITFIELD("5"),
	DATA_MSG_REQUEST("6"),
	DATA_MSG_PIECE("7");
	
	private String value;
	
	private DataMessageType(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return this.value;
	}
}