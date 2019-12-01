import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonProperties {
	
	private int numOfPreferredNeighbr;
	private int unchokingInterval;
	private int optUnchokingInterval;
	private String fileName;
	private int fileSize;
	private int pieceSize;
	
	
	public CommonProperties() {			// read Common.cfg
		String line;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("Common.cfg"));
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				if (tokens[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
					numOfPreferredNeighbr = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("UnchokingInterval")) {
					unchokingInterval = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("OptimisticUnchokingInterval")) {
					optUnchokingInterval = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("FileName")) {
					fileName = tokens[1];
				} else if (tokens[0].equalsIgnoreCase("FileSize")) {
					fileSize = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("PieceSize")) {
					pieceSize = Integer.parseInt(tokens[1]);
				}
			}

		} catch (Exception ex) {
		} finally {
			try {
				in.close();
			} catch(IOException e) {}
		}
	}

	public int getNumOfPreferredNeighbr() {
		return numOfPreferredNeighbr;
	}

	public void setNumOfPreferredNeighbr(int numOfPreferredNeighbr) {
		this.numOfPreferredNeighbr = numOfPreferredNeighbr;
	}

	public int getUnchokingInterval() {
		return unchokingInterval;
	}

	public void setUnchokingInterval(int unchokingInterval) {
		this.unchokingInterval = unchokingInterval;
	}

	public int getOptUnchokingInterval() {
		return optUnchokingInterval;
	}

	public void setOptUnchokingInterval(int optUnchokingInterval) {
		this.optUnchokingInterval = optUnchokingInterval;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public int getPieceSize() {
		return pieceSize;
	}

	public void setPieceSize(int pieceSize) {
		this.pieceSize = pieceSize;
	}
}