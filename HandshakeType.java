
public enum HandshakeType {
	
	HANDSHAKE_MSG_LEN(32),		// Handshake 메세지의 길이 32byte
	HANDSHAKE_HEADER_LEN(18),		// 18바이트의 핸드세이크 메세지 헤더
	HANDSHAKE_ZEROBITS_LEN(10),		// 10바이트의 제로비트
	HANDSHAKE_PEERID_LEN(4),		// 피어아이디 4바이트
	DATA_MSG_LEN(4),		// 4byte message length field 이부분은 actual message
	DATA_MSG_TYPE(1),		// 1byte message type field specifies the type of the message , actual message에 속함
	PIECE_INDEX_LEN(4);		// 'have' message 와 request 메세지, piece 메세지가 4 byte piece index field 를 payload 에 포함하고 있다는데 아마 그걸로 유추됨
	
	private int value;
	
	private HandshakeType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
}