package iped.parsers.external;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import iped.parsers.util.ExternalParserConfigGenerator;
import iped.parsers.util.RepoToolDownloader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

// WIP: parsing only superfetch file for now
public class ExternalParserTest implements ExternalParsersConfigReaderMetKeys {
    private static String toolsPath = "/src/test/tmp_tools/";
    private static String userDir = System.getProperty("user.dir");
    private static String absoluteToolsPath = userDir + toolsPath;
    private static String XMLFilePath = userDir + "/src/test/ExternalParsers.xml";
    private static String osName = System.getProperty("os.name").toLowerCase();
    private static File XMLFile;

    private static ExternalParserConfigGenerator createDefaultExternalParserConfig(String name, String toolPath, String checkCommand,
        String command, MediaType mimeType) throws TikaException, TransformerException {

        ExternalParserConfigGenerator parserConfigGenerator = new ExternalParserConfigGenerator();
        parserConfigGenerator.setParserName(name);
        parserConfigGenerator.setWinToolPath(toolPath);
        parserConfigGenerator.setCheckCommand(checkCommand);
        parserConfigGenerator.setErrorCodes(1);
        parserConfigGenerator.setCommand(command);
        HashSet<MediaType> mimeTypes = new HashSet<>();
        mimeTypes.add(mimeType);
        parserConfigGenerator.addMimeTypes(mimeTypes);
        parserConfigGenerator.setOutputCharset("ISO-8859-1");

        return parserConfigGenerator;
    }

    @BeforeClass
    public static void setUp() throws IOException, TikaException, TransformerException, ParserConfigurationException, SAXException {
        XMLFile = new File(XMLFilePath);

        // add SuperFetch parser configuration
        ExternalParserConfigGenerator superFetchConfigGenerator = createDefaultExternalParserConfig("SuperFetchParser",
            toolsPath + "libagdb/", "agdbinfo -V", "agdbinfo ${INPUT}", MediaType.application("x-superfetch"));
        superFetchConfigGenerator.writeDocumentToFile(XMLFile);

        // append Prefetch parser configuration to the same file
        ExternalParserConfigGenerator preFetchConfigGenerator = createDefaultExternalParserConfig("PrefetchParser",
            toolsPath + "sccainfo/", "sccainfo -V", "sccainfo ${INPUT}", MediaType.application("x-prefetch"));
        preFetchConfigGenerator.writeDocumentToFile(XMLFile);

        if (osName.startsWith("windows")) {
            String repoPath = "libyal/libagdb/20181111.1/libagdb-20181111.1.zip";
            RepoToolDownloader.unzipFromUrl(repoPath, absoluteToolsPath);
            repoPath = "libyal/sccainfo/20170205.1/sccainfo-20170205.1.zip";
            RepoToolDownloader.unzipFromUrl(repoPath, absoluteToolsPath);
        }
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (osName.startsWith("windows")) {
            FileUtils.deleteDirectory(new File(absoluteToolsPath));
        }
        XMLFile.delete();
    }
    
    @Test
    public void testSuperFetch() throws IOException, TikaException, SAXException, TransformerException {

        ExternalParser parser = ExternalParsersConfigReader.read(new FileInputStream(XMLFile)).get(0);

        ContentHandler handler = new BodyContentHandler(1 << 20);
        ParseContext context = new ParseContext();
        Metadata metadata = new Metadata();
        String fileName = "test_superfetchAgGlFgAppHistory.db";
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);

        try (InputStream stream = this.getClass().getResourceAsStream("/test-files/" + fileName)) {

            assumeTrue(osName.startsWith("windows"));
            parser.parse(stream, handler, metadata, context);
            String mts = metadata.toString();
            String hts = handler.toString();

            assertTrue(mts.contains(fileName));
            assertTrue(hts.contains("Creation time"));
            assertTrue(hts.contains("Mar 25, 2015 11:08:36.956950100 UTC"));

        }

    }
}
