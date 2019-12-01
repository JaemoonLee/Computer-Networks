public class Piece {
	
	public int isPresent;
	public String fromPeerID;
	public byte [] filePiece; 			
	public int pieceIndex;
	
	public int getIsPresent() {
		return isPresent;
	}
	public void setIsPresent(int isPresent) {
		this.isPresent = isPresent;
	}
	public String getFromPeerID() {
		return fromPeerID;
	}
	public void setFromPeerID(String fromPeerID) {
		this.fromPeerID = fromPeerID;
	}
	public Piece() {
		CommonProperties CommonProperties = new CommonProperties();
		
		filePiece = new byte[CommonProperties.getPieceSize()];		// ComonProperties 에서 PieceSize 값을 가져옴
		pieceIndex = -1;
		isPresent = 0;
		fromPeerID = null;
	}
	/**
	 * @param payload
	 * @return
	 * 
	 */
	public static Piece decodePiece(byte []payload)	{		// 바이트로 받은 piece 메세지의 payload 를 디코딩 즉 다운로딩하는것
		byte[] byteIndex = new byte[HandshakeType.PIECE_INDEX_LEN.getValue()];
		Piece piece = new Piece();
		System.arraycopy(payload, 0, byteIndex, 0, HandshakeType.PIECE_INDEX_LEN.getValue());		// piece index 에 해당하는 payload 를 카피해옴
		piece.pieceIndex = ConversionUtil.byteArrayToInt(byteIndex);		// 바이트로 되어있는 피스 인덱스를 인트로 받아옴
		piece.filePiece = new byte[payload.length-HandshakeType.PIECE_INDEX_LEN.getValue()];
		System.arraycopy(payload, HandshakeType.PIECE_INDEX_LEN.getValue(), piece.filePiece, 0, payload.length-HandshakeType.PIECE_INDEX_LEN.getValue());	// 받은 payload 를 내게 복사
		return piece;
	}
}
