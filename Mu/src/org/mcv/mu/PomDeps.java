package org.mcv.mu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PomDeps {

	private static XPathFactory xpathFactory = XPathFactory.newInstance();
	private static XPathExpression dependencyExpr = null;
	private static XPathExpression depGroupIdExpr = null;
	private static XPathExpression depArtifactIdExpr = null;
	private static XPathExpression depVersionExpr = null;
	private static XPathExpression scopeExpr = null;

	public static List<String> execute(final String pomFile) {
		try {
			XPath xpath = xpathFactory.newXPath();
			dependencyExpr = xpath.compile("/project/dependencies/dependency");
			scopeExpr = xpath.compile("./scope/text()");
			depGroupIdExpr = xpath.compile("./groupId/text()");
			depArtifactIdExpr = xpath.compile("./artifactId/text()");
			depVersionExpr = xpath.compile("./version/text()");

			File f = new File(pomFile);
			Document document;

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(f);
			List<String> dependencies = new ArrayList<>();

			NodeList nodes = (NodeList) dependencyExpr.evaluate(document, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node dependency = nodes.item(i);
				String scope = (String) scopeExpr.evaluate(dependency, XPathConstants.STRING);
				if (scope.equals("test"))
					continue;
				String depGroupId = (String) depGroupIdExpr.evaluate(dependency, XPathConstants.STRING);
				String depArtifactId = (String) depArtifactIdExpr.evaluate(dependency, XPathConstants.STRING);
				String depVersion = (String) depVersionExpr.evaluate(dependency, XPathConstants.STRING);
				String depId = depGroupId + ":" + depArtifactId + ":" + depVersion;
				dependencies.add(depId);
			}
			return dependencies;
		} catch (Exception e) {
			throw new Interpreter.InterpreterError(e);
		}
	}
}
