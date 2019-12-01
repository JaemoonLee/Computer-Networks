import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BitField {
	
	public int size;
	public Piece[] pieces;	
	public CommonProperties CommonProperties = null;
	
	public BitField() {
		CommonProperties = new CommonProperties();		// Common.cfg 를 읽어옴
		size = (int) Math.ceil(((double) CommonProperties.getFileSize() / (double) CommonProperties.getPieceSize()));	// FileSize 나누기 PieceSize 즉 Piece 의 개수
		this.pieces = new Piece[size];		// Piece[size] 라고 하면 size 변수는 Piece 클래스에 어디로 할당이 되는거지? 그냥 배열로 만든건가

		for (int i = 0; i < this.size; i++)
			this.pieces[i] = new Piece();		// Size 수만큼 piece 생성
	}
	
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	public Piece[] getPieces() {
		return pieces;
	}

	public void setPieces(Piece[] pieces) {
		this.pieces = pieces;
	}
	
	public byte[] encode() {
		return this.getBytes();
	}
	
	public static BitField decode(byte[] b) {			// 받은 bitfield 메세지를 디코딩 하는것
		BitField returnBitField = new BitField();
		for(int i = 0 ; i < b.length; i ++)	{
			int count = 7;
			while(count >= 0) {
				int test = 1 << count;
				if(i * 8 + (8-count-1) < returnBitField.size) {
					if((b[i] & (test)) != 0)
						returnBitField.pieces[i * 8 + (8-count-1)].isPresent = 1;		// piece index i 에 해당하는 piece 를 가지고 있는지 여부 판별
					else
						returnBitField.pieces[i * 8 + (8-count-1)].isPresent = 0;
				}
				count--;
			}
		}
		
		return returnBitField;
	}
	
	public synchronized boolean compare(BitField yourBitField) {		// 받은 비트필드 메세지와 나의 비트필드를 비교해서 상대방이 내가 안가지고 있는 피스를 가지고 있으면 true 를 리턴
		int yourSize = yourBitField.getSize();
		
		for (int i = 0; i < yourSize; i++) {
			if (yourBitField.getPieces()[i].getIsPresent() == 1
					&& this.getPieces()[i].getIsPresent() == 0) {
				return true;
			} else
				continue;
		}

		return false;
	}

	public synchronized int returnFirstDiff(BitField yourBitField) {      //return first bit which doesn't exist in current device 즉, 내가 가지고 있지 않은 첫번재 비트를 리턴
		int mySize = this.getSize();
		int yourSize = yourBitField.getSize();

		if (mySize >= yourSize) {
			for (int i = 0; i < yourSize; i++) {
				if (yourBitField.getPieces()[i].getIsPresent() == 1
						&& this.getPieces()[i].getIsPresent() == 0) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < mySize; i++) {
				if (yourBitField.getPieces()[i].getIsPresent() == 1
						&& this.getPieces()[i].getIsPresent() == 0) {
					return i;
				}
			}
		}
		
		return -1;		// 상대방이 가지고 있고 내가 가지고 있지 않은 경우가 없는 경우
	}

	public byte[] getBytes() {			// 비트 필드의 payload 를 byte 로 변경
		int s = this.size / 8;
		if (size % 8 != 0)
			s = s + 1;
		byte[] iP = new byte[s];
		int tempInt = 0;
		int count = 0;
		int Cnt;
		for (Cnt = 1; Cnt <= this.size; Cnt++) {
			int tempP = this.pieces[Cnt-1].isPresent;
			tempInt = tempInt << 1;
			if (tempP == 1) {
				tempInt = tempInt + 1;
			} else
				tempInt = tempInt + 0;

			if (Cnt % 8 == 0 && Cnt!=0) {
				iP[count] = (byte) tempInt;
				count++;
				tempInt = 0;
			}
		}
		if ((Cnt-1) % 8 != 0) {
			int tempShift = ((size) - (size / 8) * 8);
			tempInt = tempInt << (8 - tempShift);
			iP[count] = (byte) tempInt;
		}
		return iP;
	}
	
	public void initOwnBitfield(String OwnPeerId, int hasFile) {    // examine hasfile Field (isFirstPeer 값으로 변수가 정의되어있음)
		if (null != OwnPeerId) {
			if (hasFile != 1) {										// in case that file does not exit
				for (int i = 0; i < this.size; i++) {
					this.pieces[i].setIsPresent(0);					// 파일어없다면 pieces 배열에 isPresent 변수 값 0을 할당
					this.pieces[i].setFromPeerID(OwnPeerId);		// 피어 아이디 할당
				}
			} else {												// in case that file does exit
				for (int i = 0; i < this.size; i++) {
					this.pieces[i].setIsPresent(1);					// 파일이 있는 경우 pieces 배열에 IsPresent 변수 값 1을 할당
					this.pieces[i].setFromPeerID(OwnPeerId);
				}
			}
		}
	}

	public synchronized void updateBitField(String peerId, Piece piece) {				// 비트 필드 정보를 업데이트, Piece 메세지 즉 다운로드 함수도 여기에 정의 되어 있음 받으면서 비트필드 업데이트
		RandomAccessFile raf = null;
		
		try {
			if (peerProcess.ownBitField.pieces[piece.pieceIndex].isPresent == 1) {		// 만약 받은 해당 비트 필드 인덱스가 나한테도 있는 경우
				peerProcess.showLog(peerId + " Piece already received!!");
			} 
			else 
			{
				String fileName = CommonProperties.getFileName();
				File file = new File(peerProcess.peerID, fileName);
				int off = piece.pieceIndex * CommonProperties.getPieceSize();		// 바이트 오프셋 계산 (피스 인덱스 * 피스당 용량(바이트))
				raf = new RandomAccessFile(file, "rw");		// rw: 읽고 쓰는 옵션
				byte[] byteWrite;
				byteWrite = piece.filePiece;
				
				raf.seek(off);		// 오프셋에 해당하는 위치를 찍어서 가져옴
				raf.write(byteWrite);		// 가져온 파일을 라이트함

				this.pieces[piece.pieceIndex].setIsPresent(1);	// 해당 피스 인덱스에 해당 하는 비트 필드를 1로 업데이트
				this.pieces[piece.pieceIndex].setFromPeerID(peerId);		// 그 피스를 어느 피어로부터 받았는지 없데이트
				
				peerProcess.showLog(peerProcess.peerID
						+ " has downloaded the PIECE " + piece.pieceIndex
						+ " from Peer " + peerId
						+ ". Now the number of pieces it has is "
						+ peerProcess.ownBitField.ownPieces());

				if (peerProcess.ownBitField.isCompleted()) {		// 다운로드가 다 되어있는 확인
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(peerProcess.peerID).isInterested = 0;
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(peerProcess.peerID).isCompleted = 1;
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(peerProcess.peerID).isChoked = 0;
					updatePeerInfo(peerProcess.peerID, 1);		// Peerconfig 에 다 받으면 hasfile 필드를 1로 업데이트
					
					peerProcess.showLog(peerProcess.peerID + " has DOWNLOADED the complete file.");
				}
			}

		} catch (Exception e) {
			peerProcess.showLog(peerProcess.peerID + " EROR in updating bitfield " + e.getMessage());
		} finally {
			try {
				raf.close();
			} catch(IOException e) {
				peerProcess.showLog(peerProcess.peerID + " EROR in updating bitfield " + e.getMessage());
			} finally {}
		}
	}	

    public int ownPieces() {
        int count = 0;
        for (int i = 0; i < this.size; i++) {
            if (this.pieces[i].isPresent == 1) {
				count++;
			}			
		}
        return count;
    }
		
    public boolean isCompleted() {
        for (int i = 0; i < this.size; i++) {
            if (this.pieces[i].isPresent == 0) {
                return false;
            }
        }
        return true;
    }
	
	public void updatePeerInfo(String clientID, int hasFile) {		// PeerInfo 컨피그에 hasfile 필드를 업데이트
		BufferedWriter out = null;
		BufferedReader in = null;
		
		try {
			in= new BufferedReader(new FileReader("PeerInfo.cfg"));			// read PeerInfo.cfg
		
			String line;
			StringBuilder sb = new StringBuilder(1024);		// 스트링빌더로 append 메소드를 쓸 수 있음 (문자열 추가하는 메소드)
			String contents[] = null;
			
			while((line = in.readLine()) != null) {
				contents = line.trim().split("\\s+");
				if (contents.length != 4) {
					throw new Exception(clientID + " Error in PeerInfo.cfg");
				}
				
				if(contents[0].equals(clientID)) {
					sb.append(contents[0] + " " + contents[1] + " " + contents[2] + " " + hasFile);		// hasFile 을 받아서 업데이트. 실제 사용은 메세지 프로세서에서 다받고 hasfile 에 1을 할당하여 1이 쓰이게 만든다.
				} else {
					sb.append(line);
				}
				sb.append("\n");
			}
		
			out = new BufferedWriter(new FileWriter("PeerInfo.cfg"));
			out.write(sb.toString());
			
		} catch (Exception e) {
			peerProcess.showLog(clientID + " Error in updating the PeerInfo.cfg " +  e.getMessage());
		} finally {
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				peerProcess.showLog(clientID + " Error in Buffered close " +  e.getMessage());
			}
		}
	}
}