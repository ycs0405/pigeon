package payne.framework.pigeon.core.formatting.jaxb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import payne.framework.pigeon.core.exception.FormatterException;
import payne.framework.pigeon.core.formatting.InvocationFormatter;
import payne.framework.pigeon.core.formatting.Structure;
import payne.framework.pigeon.core.formatting.Structure.Form;
import payne.framework.pigeon.core.toolkit.IOToolkit;

public class XMLInvocationFormatter implements InvocationFormatter {
	private final JAXBContext context;

	public XMLInvocationFormatter() throws IOException, ClassNotFoundException, JAXBException {
		this("pigeon-jaxb.index");
	}

	public XMLInvocationFormatter(String path) throws IOException, ClassNotFoundException, JAXBException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL url = classLoader.getResource(path);
		InputStream index = url != null ? url.openStream() : null;
		if (index == null) {
			throw new FileNotFoundException("file " + path + " not found in classpath");
		}
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			Set<Class<?>> classes = new HashSet<Class<?>>();
			isr = new InputStreamReader(index);
			br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				// 忽略空行和注释行
				if (line.trim().equals("") || line.trim().startsWith("#")) {
					continue;
				}
				Class<?> clazz = classLoader.loadClass(line.trim());
				classes.add(clazz);
			}
			this.context = JAXBContext.newInstance(classes.toArray(new Class<?>[classes.size()]));
		} finally {
			IOToolkit.close(br);
			IOToolkit.close(isr);
			IOToolkit.close(index);
		}
	}

	public XMLInvocationFormatter(JAXBContext context) {
		this.context = context;
	}

	public String algorithm() {
		return "application/xml";
	}

	public void serialize(Object data, Structure structure, OutputStream out, String charset) throws FormatterException {
		try {
			if (structure.form == Form.ARRAY) {
				switch (structure.types.length) {
				case 0:
					data = null;
					break;
				case 1:
					data = data == null ? data : Array.get(data, 0);
					break;
				default:
					throw new IllegalArgumentException("can not marshall an array object");
				}
			}
			Marshaller marshaller = context.createMarshaller();
			marshaller.marshal(data, out);
		} catch (Exception e) {
			throw new FormatterException(e, this, data);
		}
	}

	public Object deserialize(Structure structure, InputStream in, String charset) throws FormatterException {
		try {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Object data = unmarshaller.unmarshal(in);
			if (structure.form == Form.ARRAY) {
				data = new Object[] { data };
			}
			return data;
		} catch (Exception e) {
			throw new FormatterException(e, this, in, structure);
		}
	}

}
