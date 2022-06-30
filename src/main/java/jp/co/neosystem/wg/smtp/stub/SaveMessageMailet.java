package jp.co.neosystem.wg.smtp.stub;

import jp.co.neosystem.wg.smtp.stub.conf.SmtpStubConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Header;
import javax.mail.MessagingException;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

public class SaveMessageMailet extends GenericMailet {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaveMessageMailet.class);

	@Override
	public void service(Mail mail) throws MessagingException {
		var sender = mail.getMaybeSender();
		LOGGER.info("MyCustomeMailet.service()");
		LOGGER.info("sender {}", sender.toString());

		var message = mail.getMessage();
		String directoryName = message.getMessageID().replaceAll("[<>]", "");
		SmtpStubConfig config = SmtpStubMain.getSmtpStubConfig();

		if (StringUtils.isNotEmpty(config.getDirectoryPrefixHeader())) {
			String[] headers = message.getHeader(config.getDirectoryPrefixHeader());
			if (headers != null && headers.length > 0) {
				DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
				directoryName = headers[0] + "_" + format.format(new Date());
			}
		}

		File directory = new File(config.getSaveDirectory(), directoryName);
		directory.mkdir();

		try {
			File headersFile = new File(directory, "headers.txt");
			saveHeaders(message.getAllHeaders(), headersFile);

			Object content = message.getContent();
			File contentFile = new File(directory, "content.txt");
			saveContent(content.toString(), contentFile);
		} catch (IOException e) {
			LOGGER.warn(e.getMessage(), e);
		}
		return;
	}

	private void saveContent(String content, File fileName) throws IOException {
		try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
			 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream)) {
			outputStreamWriter.write(content);
		}
		return;
	}

	private void saveHeaders(Enumeration<Header> headers, File fileName) throws IOException {
		try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
			saveHeaders(headers, fileOutputStream);
		}
		return;
	}

	private void saveHeaders(Enumeration<Header> headers, OutputStream outputStream) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(outputStream);

		for (; headers.hasMoreElements(); ) {
			Header header = headers.nextElement();
			writer.write(header.getName() + ": " + header.getValue());
			writer.write("\n");
		}
		writer.flush();
		return;
	}
}
