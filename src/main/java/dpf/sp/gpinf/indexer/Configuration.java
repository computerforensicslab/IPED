/*
 * Copyright 2012-2014, Luis Filipe da Cruz Nassif
 * 
 * This file is part of Indexador e Processador de EvidÃªncias Digitais (IPED).
 *
 * IPED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IPED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IPED.  If not, see <http://www.gnu.org/licenses/>.
 */
package dpf.sp.gpinf.indexer;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.tika.fork.ForkParser2;
import org.apache.tika.mime.MimeTypesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dpf.sp.gpinf.indexer.analysis.LetterDigitTokenizer;
import dpf.sp.gpinf.indexer.io.FastPipedReader;
import dpf.sp.gpinf.indexer.io.ParsingReader;
import dpf.sp.gpinf.indexer.parsers.EDBParser;
import dpf.sp.gpinf.indexer.parsers.IndexDatParser;
import dpf.sp.gpinf.indexer.parsers.IndexerDefaultParser;
import dpf.sp.gpinf.indexer.parsers.OCRParser;
import dpf.sp.gpinf.indexer.parsers.LibpffPSTParser;
import dpf.sp.gpinf.indexer.parsers.PDFOCRTextParser;
import dpf.sp.gpinf.indexer.parsers.RawStringParser;
import dpf.sp.gpinf.indexer.parsers.RegistryParser;
import dpf.sp.gpinf.indexer.parsers.external.ExternalParsersFactory;
import dpf.sp.gpinf.indexer.parsers.util.PDFToImage;
import dpf.sp.gpinf.indexer.process.task.VideoThumbTask;
import dpf.sp.gpinf.indexer.search.SaveStateThread;
import dpf.sp.gpinf.indexer.util.CustomLoader.CustomURLClassLoader;
import dpf.sp.gpinf.indexer.util.IOUtil;
import dpf.sp.gpinf.indexer.util.IPEDException;
import dpf.sp.gpinf.indexer.util.UTF8Properties;
import dpf.sp.gpinf.indexer.util.Util;

/**
 * Classe principal de carregamento e acesso às configurações da aplicação.
 */
public class Configuration {

  public static final String CONFIG_FILE = "IPEDConfig.txt"; //$NON-NLS-1$
  public static final String LOCAL_CONFIG = "LocalConfig.txt"; //$NON-NLS-1$
  public static final String EXTRA_CONFIG_FILE = "AdvancedConfig.txt"; //$NON-NLS-1$
  public static final String PARSER_CONFIG = "ParserConfig.xml"; //$NON-NLS-1$
  public static final String EXTERNAL_PARSERS = "ExternalParsers.xml"; //$NON-NLS-1$
  public static final String CUSTOM_MIMES_CONFIG = "CustomSignatures.xml"; //$NON-NLS-1$

  public static UTF8Properties properties = new UTF8Properties();
  public static File indexTemp, indexerTemp;
  public static int numThreads;
  public static int textSplitSize = 100000000;
  public static int textOverlapSize = 10000;
  public static int timeOut = 180;
  public static int timeOutPerMB = 1;
  public static boolean forceMerge = true;
  public static String configPath, appRoot;
  public static boolean embutirLibreOffice = true;
  public static boolean addUnallocated = false;
  public static boolean addFileSlacks = false;
  public static long unallocatedFragSize = 1024 * 1024 * 1024;
  public static long minItemSizeToFragment = 100 * 1024 * 1024;
  public static boolean indexTempOnSSD = false;
  public static boolean outputOnSSD = false;
  public static boolean entropyTest = true;
  public static boolean addFatOrphans = true;
  public static long minOrphanSizeToIgnore = -1;
  public static int searchThreads = 1;
  public static boolean robustImageReading = false;
  public static String phoneParsersToUse = "external"; //$NON-NLS-1$
  public static File optionalJarDir;
  public static File tskJarFile;
  public static String loaddbPathWin;
  public static Locale locale = Locale.getDefault();
  public static boolean autoManageCols = true;
  public static boolean storeTermVectors = true;
  public static int maxTokenLength = 255;
  public static boolean filterNonLatinChars = false;
  public static boolean preOpenImagesOnSleuth = false;
  public static boolean openImagesCacheWarmUpEnabled = false;
  public static int openImagesCacheWarmUpThreads = 256;
  
  private static AtomicBoolean loaded = new AtomicBoolean();
  
  private static String getAppRoot(String configPath){
	  String appRoot = new File(configPath).getAbsolutePath();
	  if(appRoot.contains("profiles")) //$NON-NLS-1$
	   	appRoot = new File(appRoot).getParentFile().getParentFile().getParent();
	  return appRoot;
  }

  /**
   * Configurações a partir do caminho informado.
   */
  public static void getConfiguration(String configPathStr) throws Exception {
	  
	  if(loaded.getAndSet(true))
		  return;

    // DataSource.testConnection(configPathStr);
    LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", NoOpLog.class.getName()); //$NON-NLS-1$
    
    Logger LOGGER = null;
    if(Configuration.class.getClassLoader().getClass().getName()
            .equals(CustomURLClassLoader.class.getName()))
        LOGGER = LoggerFactory.getLogger(Configuration.class);
    
    if(LOGGER != null) LOGGER.info("Loading configuration from " + configPathStr); //$NON-NLS-1$

    configPath = configPathStr;
    appRoot = getAppRoot(configPath);

    System.setProperty("tika.config", configPath + "/conf/" + PARSER_CONFIG); //$NON-NLS-1$ //$NON-NLS-2$
    System.setProperty(ExternalParsersFactory.EXTERNAL_PARSER_PROP, configPath + "/conf/" + EXTERNAL_PARSERS); //$NON-NLS-1$
    System.setProperty(MimeTypesFactory.CUSTOM_MIMES_SYS_PROP, appRoot + "/conf/" + Configuration.CUSTOM_MIMES_CONFIG); //$NON-NLS-1$

    properties.load(new File(appRoot + "/" + LOCAL_CONFIG)); //$NON-NLS-1$
    properties.load(new File(configPath + "/" + CONFIG_FILE)); //$NON-NLS-1$
    properties.load(new File(configPath + "/conf/" + EXTRA_CONFIG_FILE)); //$NON-NLS-1$

    String value;

    if (System.getProperty("java.io.basetmpdir") == null) { //$NON-NLS-1$
        System.setProperty("java.io.basetmpdir", System.getProperty("java.io.tmpdir")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    File newTmp = null, tmp = new File(System.getProperty("java.io.basetmpdir")); //$NON-NLS-1$

    value = properties.getProperty("indexTemp"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (indexerTemp == null) {
      if (value != null && !value.equalsIgnoreCase("default")) { //$NON-NLS-1$
        newTmp = new File(value);
        if (!newTmp.exists() && !newTmp.mkdirs()) {
            if(LOGGER != null) LOGGER.info("Fail to create temp directory" + newTmp.getAbsolutePath()); //$NON-NLS-1$
        } else {
          tmp = newTmp;
        }
      }
      indexerTemp = new File(tmp, "indexador-temp" + new Date().getTime()); //$NON-NLS-1$
      if (!indexerTemp.mkdirs()) {
        tmp = new File(System.getProperty("java.io.basetmpdir")); //$NON-NLS-1$
        indexerTemp = new File(tmp, "indexador-temp" + new Date().getTime()); //$NON-NLS-1$
        indexerTemp.mkdirs();
      }
      if (indexerTemp.exists()) {
        System.setProperty("java.io.tmpdir", indexerTemp.getAbsolutePath()); //$NON-NLS-1$
      }
      if (tmp == newTmp) {
        indexTemp = new File(indexerTemp, "index"); //$NON-NLS-1$
      }
    }
    if (indexerTemp != null) {
      indexerTemp.mkdirs();
    }
    ConstantsViewer.indexerTemp = indexerTemp;

    value = properties.getProperty("robustImageReading"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      robustImageReading = Boolean.valueOf(value);
    }

    value = properties.getProperty("numThreads"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.equalsIgnoreCase("default")) { //$NON-NLS-1$
      numThreads = Integer.valueOf(value);
    } else {
      numThreads = Runtime.getRuntime().availableProcessors();
    }
    
    value = properties.getProperty("enableExternalParsing"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      ForkParser2.enabled = Boolean.valueOf(value.trim());
    }
    
    value = properties.getProperty("numExternalParsers"); //$NON-NLS-1$
    if (value != null && !value.trim().equalsIgnoreCase("auto")) { //$NON-NLS-1$
      ForkParser2.SERVER_POOL_SIZE = Integer.valueOf(value.trim());
    }else
      ForkParser2.SERVER_POOL_SIZE = numThreads;
    
    value = properties.getProperty("externalParsingMaxMem"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      ForkParser2.SERVER_MAX_HEAP = value.trim();
    }
    
    value = properties.getProperty("locale"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty())
      locale = Locale.forLanguageTag(value.trim());
    
    System.setProperty("iped-locale", locale.toLanguageTag()); //$NON-NLS-1$

    value = properties.getProperty("forceMerge"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && value.equalsIgnoreCase("false")) { //$NON-NLS-1$
      forceMerge = false;
    }

    value = properties.getProperty("timeOut"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      timeOut = Integer.valueOf(value);
    }
    
    value = properties.getProperty("timeOutPerMB"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
    	timeOutPerMB = Integer.valueOf(value);
    }    

    value = properties.getProperty("textSplitSize"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
    	textSplitSize = Integer.valueOf(value.trim());
    }
    
    ParsingReader.setTextSplitSize(textSplitSize);
    ParsingReader.setTextOverlapSize(textOverlapSize);
    FastPipedReader.setTimeout(timeOut);

    value = properties.getProperty("entropyTest"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      entropyTest = Boolean.valueOf(value.trim());
      System.setProperty(IndexerDefaultParser.ENTROPY_TEST_PROP, value.trim());
    }

    value = properties.getProperty("indexUnknownFiles"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        System.setProperty(IndexerDefaultParser.FALLBACK_PARSER_PROP, value.trim());
    }
    
    value = properties.getProperty("indexCorruptedFiles"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        System.setProperty(IndexerDefaultParser.ERROR_PARSER_PROP, value.trim());
    }
    
    value = properties.getProperty("minRawStringSize"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(RawStringParser.MIN_STRING_SIZE, value.trim());
    }

    value = properties.getProperty("enableOCR"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        System.setProperty(OCRParser.ENABLE_PROP, value.trim());
    }

    value = properties.getProperty("OCRLanguage"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(OCRParser.LANGUAGE_PROP, value.trim());
    }

    value = properties.getProperty("minFileSize2OCR"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(OCRParser.MIN_SIZE_PROP, value.trim());
    }

    value = properties.getProperty("maxFileSize2OCR"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(OCRParser.MAX_SIZE_PROP, value.trim());
    }

    value = properties.getProperty("pageSegMode"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(OCRParser.PAGE_SEGMODE_PROP, value.trim());
    }

    value = properties.getProperty("maxPDFTextSize2OCR"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFOCRTextParser.MAX_CHARS_TO_OCR, value.trim());
    }
    
    value = properties.getProperty("processImagesInPDFs"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFOCRTextParser.PROCESS_INLINE_IMAGES, value.trim());
    }
    
    value = properties.getProperty("sortPDFChars"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFOCRTextParser.SORT_PDF_CHARS, value.trim());
    }

    value = properties.getProperty("pdfToImgResolution"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFToImage.RESOLUTION_PROP, value.trim());
    }

    value = properties.getProperty("pdfToImgLib"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFToImage.PDFLIB_PROP, value.trim());
    }
    
    value = properties.getProperty("externalPdfToImgConv"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFToImage.EXTERNAL_CONV_PROP, value.trim());
    }
    //do not open extra processes for OCR if forkParser is enabled
    if(ForkParser2.enabled) {
        System.setProperty(PDFToImage.EXTERNAL_CONV_PROP, "false");
    }
    
    value = properties.getProperty("externalConvMaxMem"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
      System.setProperty(PDFToImage.EXTERNAL_CONV_MAXMEM_PROP, value.trim()); 
    }
    
    value = properties.getProperty("embutirLibreOffice"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      embutirLibreOffice = Boolean.valueOf(value);
    }

    value = properties.getProperty("extraCharsToIndex"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      LetterDigitTokenizer.load(value);
    }

    value = properties.getProperty("convertCharsToLowerCase"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      LetterDigitTokenizer.convertCharsToLowerCase = Boolean.valueOf(value);
    }

    value = properties.getProperty("addUnallocated"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      addUnallocated = Boolean.valueOf(value);
    }
    
    value = properties.getProperty("addFileSlacks"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      addFileSlacks = Boolean.valueOf(value);
    }

    value = properties.getProperty("unallocatedFragSize"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      unallocatedFragSize = Long.valueOf(value);
    }
    
    value = properties.getProperty("minItemSizeToFragment"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
    	minItemSizeToFragment = Long.valueOf(value);
    }

    value = properties.getProperty("indexTempOnSSD"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      indexTempOnSSD = Boolean.valueOf(value);
    }
    
    value = properties.getProperty("outputOnSSD"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      outputOnSSD = Boolean.valueOf(value);
    }
    if(outputOnSSD)
    	indexTemp = null;

    value = properties.getProperty("addFatOrphans"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      addFatOrphans = Boolean.valueOf(value);
    }

    value = properties.getProperty("minOrphanSizeToIgnore"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      minOrphanSizeToIgnore = Long.valueOf(value);
    }

    value = properties.getProperty("searchThreads"); //$NON-NLS-1$
    if (value != null) {
      value = value.trim();
    }
    if (value != null && !value.isEmpty()) {
      searchThreads = Integer.valueOf(value);
    }
    
    value = properties.getProperty("maxBackups"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        SaveStateThread.MAX_BACKUPS = Integer.valueOf(value.trim());
    }
    
    value = properties.getProperty("backupInterval"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        SaveStateThread.BKP_INTERVAL = Long.valueOf(value.trim());
    }
    
    value = properties.getProperty("phoneParsersToUse"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        phoneParsersToUse = value.trim();
    }
    
    value = properties.getProperty("autoManageCols"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        autoManageCols = Boolean.valueOf(value.trim());
    }
    
    value = properties.getProperty("storeTermVectors"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        storeTermVectors = Boolean.valueOf(value.trim());
    }
    
    value = properties.getProperty("maxTokenLength"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        maxTokenLength = Integer.valueOf(value.trim());
    }
    
    value = properties.getProperty("filterNonLatinChars"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        filterNonLatinChars = Boolean.valueOf(value.trim());
    }

    value = properties.getProperty("preOpenImagesOnSleuth"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        preOpenImagesOnSleuth = Boolean.valueOf(value.trim());
    }
    
    value = properties.getProperty("openImagesCacheWarmUpEnabled"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        openImagesCacheWarmUpEnabled = Boolean.valueOf(value.trim());
    }

    value = properties.getProperty("openImagesCacheWarmUpThreads"); //$NON-NLS-1$
    if (value != null && !value.trim().isEmpty()) {
        openImagesCacheWarmUpThreads = Integer.parseInt(value.trim());
    }

    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) { //$NON-NLS-1$ //$NON-NLS-2$

      String arch = "x86"; //$NON-NLS-1$
      if(System.getProperty("os.arch").contains("64")) //$NON-NLS-1$ //$NON-NLS-2$
    	  arch = "x64"; //$NON-NLS-1$
      
      loaddbPathWin = appRoot + "/tools/tsk/" + arch + "/tsk_loaddb"; //$NON-NLS-1$ //$NON-NLS-2$

      File nativelibs = new File(loaddbPathWin).getParentFile().getParentFile();
      nativelibs = new File(nativelibs, arch);
      
      IOUtil.copiaDiretorio(nativelibs, new File(indexerTemp, "nativelibs"), true); //$NON-NLS-1$
      Util.loadNatLibs(new File(indexerTemp, "nativelibs")); //$NON-NLS-1$
      
      System.setProperty(OCRParser.TOOL_PATH_PROP, appRoot + "/tools/tesseract"); //$NON-NLS-1$
      System.setProperty(EDBParser.TOOL_PATH_PROP, appRoot + "/tools/esedbexport/"); //$NON-NLS-1$
      System.setProperty(LibpffPSTParser.TOOL_PATH_PROP, appRoot + "/tools/pffexport/"); //$NON-NLS-1$
      System.setProperty(IndexDatParser.TOOL_PATH_PROP, appRoot + "/tools/msiecfexport/"); //$NON-NLS-1$
      
      String mplayerPath = properties.getProperty("mplayerPath"); //$NON-NLS-1$
      if(mplayerPath != null)
          VideoThumbTask.mplayerWin = mplayerPath.trim();
    
    }else{
    	String tskJarPath = properties.getProperty("tskJarPath"); //$NON-NLS-1$
    	if (tskJarPath != null && !tskJarPath.isEmpty())
        	tskJarPath = tskJarPath.trim();
    	else
    		throw new IPEDException("You must set tskJarPath on LocalConfig.txt!"); //$NON-NLS-1$
    	
    	tskJarFile = new File(tskJarPath);
    	if(!tskJarFile.exists())
    		throw new IPEDException("File not found " + tskJarPath + ". Set tskJarPath on LocalConfig.txt!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    String optional_jars = properties.getProperty("optional_jars"); //$NON-NLS-1$
    if(optional_jars != null) {
        optionalJarDir = new File(appRoot + "/" + optional_jars.trim()); //$NON-NLS-1$
        ForkParser2.plugin_dir = optionalJarDir.getCanonicalPath();
    }

    String regripperFolder = properties.getProperty("regripperFolder"); //$NON-NLS-1$
    if(regripperFolder != null)
        System.setProperty(RegistryParser.TOOL_PATH_PROP, appRoot + "/" + regripperFolder.trim()); //$NON-NLS-1$

  }

}
