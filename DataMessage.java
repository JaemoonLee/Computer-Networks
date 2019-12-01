import java.io.UnsupportedEncodingException;

public class DataMessage {
	
    private String messageType;
    private String messageLength;
    private int dataLength = HandshakeType.DATA_MSG_TYPE.getValue();		// 메세지가 어떤 type의 message 인지 값을 가져옴
    private byte[] type = null;
	private byte[] len = null;
	private byte[] payload = null;
	
	public DataMessage(){}
	
    public DataMessage(String Type) {
    	setDataMessage(Type);
    }

	public DataMessage(String Type, byte[] Payload) {		
		try	{
			if (Payload != null) {			// 메세지 길이 에러 케이스
                this.setMessageLength(Payload.length + 1);
                if (this.len.length > HandshakeType.DATA_MSG_LEN.getValue())
                    throw new Exception("DataMessage:: Constructor - Message length is too large.");
                
                this.setPayload(Payload);		// 메세지 길이 에러가 아니라면 Payload 를 받아옴
			} else {
				setDataMessage(Type);		// Payload 가 없다면 setDataMessage 호출
			}

			this.setMessageType(Type);		// String 배열인 Type 을 byte 배열로 바꿔서 바이트 변수 type 에 저장
			if (this.getMessageType().length > HandshakeType.DATA_MSG_TYPE.getValue())
				throw new Exception("DataMessage:: Constructor - Type length is too large.");
		} catch (Exception e) {
			peerProcess.showLog(e.toString());
		}
	}

    public void setDataMessage(String Type){
    	try {
            if (DataMessageType.DATA_MSG_CHOKE.getValue().equals(Type) 			// Payload 가 없는 4가지 메세지 중 하나를 구별
            		|| DataMessageType.DATA_MSG_UNCHOKE.getValue().equals(Type)
            		|| DataMessageType.DATA_MSG_INTERESTED.getValue().equals(Type) 
            		|| DataMessageType.DATA_MSG_NOTINTERESTED.getValue().equals(Type)) {
                this.setMessageLength(1);		// Payload 가 없기 때문에 총 메세지 길이를 1 byte 로 설정해줌 (메세지 length 필드 자체는 포함이 안되어있기 때문)
                this.setMessageType(Type);		// String 배열인 Type 을 byte 배열로 바꿔서 바이트 변수 type 에 저장
                this.payload = null;			// Payload 가 없기 때문에 null 로 설정
            } else
            	throw new Exception("DataMessage:: Constructor - Wrong constructor selection.");
        } catch (Exception e) {
            peerProcess.showLog(e.toString());
        }
    }
    
    public void setMessageLength(int messageLength) {
        this.dataLength = messageLength;		// 메세지 타입을 datalength 에 넣어줌
        this.messageLength = String.valueOf(messageLength);		// valueOf: 배열의 구성요소를 문자열로 돌려줌. 즉 int형의 messageLength를 문자열 형태로 저장함
        this.len = ConversionUtil.intToByteArray(messageLength);		// 바이트어레이로 변경하여 len에 넣음
    }
	
	public void setMessageLength(byte[] len) {
		Integer l = ConversionUtil.byteArrayToInt(len);		// 바이트어레이로 받은 len 값을 integer로 변경
		this.messageLength = String.valueOf(l);
		this.len = len;
		this.dataLength = l;  
	}

	public byte[] getMessageLength() {
		return len;
	}
	
	public String getMessageLengthString() {
		return messageLength;
	}

	public int getMessageLengthInt() {
		return this.dataLength;
	}

	public void setMessageType(byte[] type) {		// 바이트 타입의 type 을 문자열 messageType 으로 저장
		try {
			this.messageType = new String(type, MessageConstants.MSG_CHARSET_NAME);		//바이트로 받은 type 을 문자열로 변경해서 messageType 변수에 저장
			this.type = type;
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
	}
	
	public void setMessageType(String messageType) {		// 문자열 messageType 와 바이트 Type 에 저장함
		try {
			this.messageType = messageType.trim();		// trim 으로 문자열의 앞, 뒤 공백 제거
			this.type = this.messageType.getBytes(MessageConstants.MSG_CHARSET_NAME);	// getBytes 로 문자열을 바이트 배열로 변환해서 바이트 변수 type 에 저장. 네트워크로 문자열을 전송하는 경우 사용
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
	}
	
	public byte[] getMessageType() {
		return type;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getMessageTypeString() {
		return messageType;
	}

	@Override
	public String toString() {
		String str = null;
		try {
			str = "[DataMessage] : Message Length - "
					+ this.messageLength
					+ ", Message Type - "
					+ this.messageType
					+ ", Data - "
					+ (new String(this.payload, MessageConstants.MSG_CHARSET_NAME)).toString().trim();
		} catch (UnsupportedEncodingException e) {
			peerProcess.showLog(e.toString());
		}
		return str;
	}

    public static byte[] encodeMessage(DataMessage msg) {			// msg 를 바이트 타입으로 전환, 메세지를 보내기 위해서
        byte[] msgStream = null;
        int msgType;
        
        try {
        	if (StringUtil.isEmpty(msg.getMessageTypeString())) {		// messageType 이 empty 라면 true 를 리턴 즉 오류 케이스
        		throw new Exception("Invalid MessageTypeString.");
        	}
        	
            msgType = Integer.parseInt(msg.getMessageTypeString());		// 문자열 타입은 messageType 을 정수로 변환해서 저장
            
            if (msg.getMessageLength() == null)
            	throw new Exception(MessageConstants.ERR_MSG_SIZE);
            else if (msg.getMessageType() == null)
            	throw new Exception(MessageConstants.ERR_MSG_TYPE);
            else if (msg.getMessageLength().length > HandshakeType.DATA_MSG_LEN.getValue())
                throw new Exception(MessageConstants.ERR_MSG_SIZE);
            else if (msgType < 0 || msgType > 7)
                throw new Exception(MessageConstants.ERR_MSG_TYPE);
            
            if (msg.getPayload()!= null) {		// msg 의 Payload 가 있는 경우
                msgStream = new byte[HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue() + msg.getPayload().length];		// actual message 바이트 배열을 만듬(length, type, payload를 가진)
                
                System.arraycopy(msg.getMessageLength(), 0, msgStream, 0, msg.getMessageLength().length);	// message length 필드를 카피
                System.arraycopy(msg.getMessageType(), 0, msgStream, HandshakeType.DATA_MSG_LEN.getValue(), HandshakeType.DATA_MSG_TYPE.getValue());	// message Type 필드 카피
                System.arraycopy(msg.getPayload(), 0, msgStream, HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue(), msg.getPayload().length);	// payload 카피
            } else {	// msg 의 payload 가 없는 경우
                msgStream = new byte[HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue()];
                
                System.arraycopy(msg.getMessageLength(), 0, msgStream, 0, msg.getMessageLength().length);
                System.arraycopy(msg.getMessageType(), 0, msgStream, HandshakeType.DATA_MSG_LEN.getValue(), HandshakeType.DATA_MSG_TYPE.getValue());
            }
        } catch (Exception e) {
            peerProcess.showLog(e.toString());
            msgStream = null;
        }
        
        return msgStream;		// 바이트 메세지 스트림을 리턴
    }
	
	public static DataMessage decodeMessage(byte[] Message) {		// 받은 메세지를 디코딩
		DataMessage msg = new DataMessage();		// DataMessage 컨스트럭터 생성
		byte[] msgLength = new byte[HandshakeType.DATA_MSG_LEN.getValue()];		// 바이트 타입의 len 을 받아옴
		byte[] msgType = new byte[HandshakeType.DATA_MSG_TYPE.getValue()];		// 바이트로 되어 있는 타입을 받아옴
		byte[] payLoad = null;
		int len;

		try {
			if (Message == null)		// 오류케이스 메세지가 비어있는 경우
				throw new Exception(MessageConstants.ERR_DATA);
			else if (Message.length < HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue())
				throw new Exception(MessageConstants.ERR_BYTE_SIZE_SMALL);

			System.arraycopy(Message, 0, msgLength, 0, HandshakeType.DATA_MSG_LEN.getValue());	// megLength 바이트 배열에 받은걸 카피
			System.arraycopy(Message, HandshakeType.DATA_MSG_LEN.getValue(), msgType, 0, HandshakeType.DATA_MSG_TYPE.getValue());		// 메세지 타입 카피

			msg.setMessageLength(msgLength);
			msg.setMessageType(msgType);
			
			len = ConversionUtil.byteArrayToInt(msgLength);
			
			if (len > 1) {		// len 이 1보다 크다는것은 즉 payload 가 있다는 뜻(len 은 message type 과 payload 합한 길이)
				payLoad = new byte[len-1];
				System.arraycopy(Message, HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue(),	payLoad, 0, Message.length - HandshakeType.DATA_MSG_LEN.getValue() - HandshakeType.DATA_MSG_TYPE.getValue());
				msg.setPayload(payLoad);	// Payload를 받아서 카피해옴
			}
			
			payLoad = null;
		} catch (Exception e) {
			peerProcess.showLog(e.toString());
			msg = null;
		}
		
		return msg;
	}
}