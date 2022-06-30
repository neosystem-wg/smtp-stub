package jp.co.neosystem.wg.smtp.stub.conf;

public class ResponseConfig {

	private String dst;

	private int returnCode = 500;

	private String smtpDescription = "error";

	public String getDst() {
		return dst;
	}

	public void setDst(String dst) {
		this.dst = dst;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public String getSmtpDescription() {
		return smtpDescription;
	}

	public void setSmtpDescription(String smtpDescription) {
		this.smtpDescription = smtpDescription;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("dst: ")
				.append(dst)
				.append(", returnCode: ")
				.append(returnCode)
				.append(", smtpDescription: ")
				.append(smtpDescription);
		return builder.toString();
	}
}
