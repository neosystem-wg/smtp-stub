package jp.co.neosystem.wg.smtp.stub.conf;

import jp.co.neosystem.wg.smtp.stub.conf.ResponseConfig;

import java.util.List;

public class SmtpStubConfig {

	private String saveDirectory = "./";

	private String directoryPrefixHeader;

	private List<ResponseConfig> response;

	public List<ResponseConfig> getResponse() {
		return response;
	}

	public void setResponse(List<ResponseConfig> response) {
		this.response = response;
	}

	public String getSaveDirectory() {
		return saveDirectory;
	}

	public void setSaveDirectory(String saveDirectory) {
		this.saveDirectory = saveDirectory;
	}

	public String getDirectoryPrefixHeader() {
		return directoryPrefixHeader;
	}

	public void setDirectoryPrefixHeader(String directoryPrefixHeader) {
		this.directoryPrefixHeader = directoryPrefixHeader;
	}
}
