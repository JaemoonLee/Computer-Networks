
import java.io.UnsupportedEncodingException;

public class HandshakeMessage extends MessageConstants {

	private byte[] header = new byte[HandshakeType.HANDSHAKE_HEADER_LEN.getValue()];		// 32 바이트 핸드세이크 메세지 헤더 생성
	private byte[] peerID = new byte[HandshakeType.HANDSHAKE_PEERID_LEN.getValue()];		// 4바이트짜리 피어아이디 필드 생성
	private byte[] zeroBits = new byte[HandshakeType.HANDSHAKE_ZEROBITS_LEN.getValue()];	// 10 바이트의 제로 비트 메세지 생성
	private String messageHeader;
	private String messagePeerID;

	public HandshakeMessage(){
	}
	
	public HandshakeMessage(String Header, String PeerId) {
		try {
			this.messageHeader = Header;
			this.header = Header.getBytes(MessageConstants.MSG_CHARSET_NAME);
			if (this.header.length > HandshakeType.HANDSHAKE_HEADER_LEN.getValue())
				throw new Exception(ERR_HEADER_SIZE);

			this.messagePeerID = PeerId;
			this.peerID = PeerId.getBytes(MessageConstants.MSG_CHARSET_NAME);
			if (this.peerID.length > HandshakeType.HANDSHAKE_PEERID_LEN.getValue())
				throw new Exception(ERR_PEER_ID_SIZE);

			this.zeroBits = "0000000000".getBytes(MessageConstants.MSG_CHARSET_NAME);
		} catch (Exception e) {
			peerProcess.showLog(e.toString());
		}

	}

	public void setHeader(byte[] handShakeHeader) {
		try {
			this.messageHeader = (new String(handShakeHeader, MessageConstants.MSG_CHARSET_NAME)).toString().trim();
			this.header = this.messageHeader.getBytes();
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
	}

	public void setPeerID(byte[] peerID) {
		try {
			this.messagePeerID = (new String(peerID, MessageConstants.MSG_CHARSET_NAME)).toString().trim();
			this.peerID = this.messagePeerID.getBytes();
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
	}
	
	public void setPeerID(String messagePeerID) {
		try {
			this.messagePeerID = messagePeerID;
			this.peerID = messagePeerID.getBytes(MessageConstants.MSG_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
	}

	public void setHeader(String messageHeader) {
		try {
			this.messageHeader = messageHeader;
			this.header = messageHeader.getBytes(MessageConstants.MSG_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
	}
	
	public byte[] getHeader() {
		return header;
	}
	
	public byte[] getPeerID() {
		return peerID;
	}

	public void setZeroBits(byte[] zeroBits) {
		this.zeroBits = zeroBits;
	}

	public byte[] getZeroBits() {
		return zeroBits;
	}

	public String getHeaderString() {
		return messageHeader;
	}

	public String getPeerIDString() {
		return messagePeerID;
	}

	public String toString() {
		return ("[HandshakeMessage] : Peer Id - " + this.messagePeerID
				+ ", Header - " + this.messageHeader);
	}

	public static HandshakeMessage decodeMessage(byte[] receivedMessage) {		// decode the byte arrary of HandshakeMessage and load to the object HandshakeMessage

		HandshakeMessage handshakeMessage = null;
		byte[] msgHeader = null;
		byte[] msgPeerID = null;

		try {
			if (receivedMessage.length != HandshakeType.HANDSHAKE_MSG_LEN.getValue())		// check initial state 에러 체킹
				throw new Exception(ERR_BYTE_ARR_SIZE);

			handshakeMessage = new HandshakeMessage();										// 핸드세이크 만듬
			msgHeader = new byte[HandshakeType.HANDSHAKE_HEADER_LEN.getValue()];
			msgPeerID = new byte[HandshakeType.HANDSHAKE_PEERID_LEN.getValue()];

			System.arraycopy(receivedMessage, 0, msgHeader, 0,										// decode the received message
					HandshakeType.HANDSHAKE_HEADER_LEN.getValue());
			System.arraycopy(receivedMessage, HandshakeType.HANDSHAKE_HEADER_LEN.getValue()
					+ HandshakeType.HANDSHAKE_ZEROBITS_LEN.getValue(), msgPeerID, 0,
					HandshakeType.HANDSHAKE_PEERID_LEN.getValue());

			handshakeMessage.setHeader(msgHeader);
			handshakeMessage.setPeerID(msgPeerID);		// 바이트로 받은 핸드세이크를 디코드해서 값을 넣어줌

		} catch (Exception e) {
			peerProcess.showLog(e.toString());
			handshakeMessage = null;
		}
		return handshakeMessage;
	}

	public static byte[] encodeMessage(HandshakeMessage handshakeMessage) {			// 핸드세이크 메세지를 보내기 위한 인코딩

		byte[] sendMessage = new byte[HandshakeType.HANDSHAKE_MSG_LEN.getValue()];

		try {
			if (handshakeMessage.getHeader() == null) {					// encode header
				throw new Exception(ERR_HEADER);
			}
			if (handshakeMessage.getHeader().length > HandshakeType.HANDSHAKE_HEADER_LEN.getValue()|| handshakeMessage.getHeader().length == 0)
			{
				throw new Exception(ERR_HEADER);
			} else {
				System.arraycopy(handshakeMessage.getHeader(), 0, sendMessage,
						0, handshakeMessage.getHeader().length);
			}

			if (handshakeMessage.getZeroBits() == null) {					// encode zero bits
				throw new Exception(ERR_ZERO_BITS);
			} 
			if (handshakeMessage.getZeroBits().length > HandshakeType.HANDSHAKE_ZEROBITS_LEN.getValue()
					|| handshakeMessage.getZeroBits().length == 0) {
				throw new Exception(ERR_ZERO_BITS);
			} else {
				System.arraycopy(handshakeMessage.getZeroBits(), 0,
						sendMessage, HandshakeType.HANDSHAKE_HEADER_LEN.getValue(),
						HandshakeType.HANDSHAKE_ZEROBITS_LEN.getValue() - 1);
			}

			if (handshakeMessage.getPeerID() == null) 					// encode PeerID
			{
				throw new Exception(ERR_PEER_ID);
			} 
			else if (handshakeMessage.getPeerID().length > HandshakeType.HANDSHAKE_PEERID_LEN.getValue()
					|| handshakeMessage.getPeerID().length == 0) 
			{
				throw new Exception(ERR_PEER_ID);
			} 
			else 
			{
				System.arraycopy(handshakeMessage.getPeerID(), 0, sendMessage,
						HandshakeType.HANDSHAKE_HEADER_LEN.getValue() + HandshakeType.HANDSHAKE_ZEROBITS_LEN.getValue(),
						handshakeMessage.getPeerID().length);
			}

		} 
		catch (Exception e) 
		{
			peerProcess.showLog(e.toString());
			sendMessage = null;
		}

		return sendMessage;
	}
}