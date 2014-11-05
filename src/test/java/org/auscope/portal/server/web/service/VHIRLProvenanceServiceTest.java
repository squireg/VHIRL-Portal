package org.auscope.portal.server.web.service;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.auscope.portal.core.cloud.CloudFileInformation;
import org.auscope.portal.core.services.cloud.CloudStorageService;
import org.auscope.portal.core.test.PortalTestClass;
import org.auscope.portal.server.vegl.VEGLJob;
import org.auscope.portal.server.vegl.VglDownload;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VHIRLProvenanceServiceTest extends PortalTestClass {
    VEGLJob preparedJob;
    final String serverURL = "http://portal-fake.vhirl.org";
    final Model plainModel = ModelFactory.createDefaultModel();
    final CloudFileInformation fileInformation = context.mock(CloudFileInformation.class);
    final int jobID = 1;
    final String cloudKey = "cloudKey";
    final String cloudServiceID = "fluffy Cloud";
    final String jobName = "Cool Job";
    final String jobDescription = "Some job I made.";
    final String activityFileName = "activity.ttl";
    List<VglDownload> downloads = new ArrayList<>();

    final String initalTurtle = "<http://portal-fake.vhirl.org/getJobObject.do?jobId=1>\n" +
            "      <http://www.w3.org/2000/01/rdf-schema#type>\n" +
            "              \"http://www.w3.org/ns/prov#Activity\" ;\n" +
            "      <http://purl.org/dc/elements/1.1/description>\n" +
            "              \"Some job I made.\"^^<http://www.w3.org/2001/XMLSchema#string> ;\n" +
            "      <http://purl.org/dc/elements/1.1/title>\n" +
            "              \"Cool Job\"^^<http://www.w3.org/2001/XMLSchema#string> ;\n";

    final String intermediateTurtle = "<http://www.w3.org/2001/XMLSchema#dateTime> ;\n" +
            "      <http://www.w3.org/ns/prov#used>\n" +
            "              \"http://portal-uploads.vhirl.org/file1\"^^<http://www.w3.org/2001/XMLSchema#string> ;\n" +
            "      <http://www.w3.org/ns/prov#wasAttributedTo>\n" +
            "              \"http://portal-fake.vhirl.org\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "\n" +
            "<http://portal-uploads.vhirl.org/file1>\n" +
            "      <http://www.w3.org/2000/01/rdf-schema#type>\n" +
            "              \"http://www.w3.org/ns/prov#Entity\" .";

    final String dateAttributionTurtle = "<http://www.w3.org/2001/XMLSchema#dateTime> ;\n" +
            "      <http://www.w3.org/ns/prov#used>\n" +
            "              \"http://portal-uploads.vhirl.org/file1\"^^<http://www.w3.org/2001/XMLSchema#string> ;\n" +
            "      <http://www.w3.org/ns/prov#wasAttributedTo>\n" +
            "              \"http://portal-fake.vhirl.org\"^^<http://www.w3.org/2001/XMLSchema#string> .\n";
    final String file1Turtle =
            "<http://portal-uploads.vhirl.org/file1>\n" +
            "      <http://www.w3.org/2000/01/rdf-schema#type>\n" +
            "              \"http://www.w3.org/ns/prov#Entity\" .\n";
    final String cloudKeyTurtle =
            "<http://portal-fake.vhirl.org/secure/jobFile.do?jobId=1&key=cloudKey>\n" +
            "      <http://www.w3.org/2000/01/rdf-schema#type>\n" +
            "              \"http://www.w3.org/ns/prov#Entity\" .";
    final String generatedTurtle =
            "      <http://www.w3.org/ns/prov#generated>\n" +
            "              \"http://portal-fake.vhirl.org/secure/jobFile.do?jobId=1&key=cloudKey\"^^<http://www.w3.org/2001/XMLSchema#string>";



    VHIRLProvenanceService vhirlProvenanceService;

    @Before
    public void setUp() throws Exception {
        preparedJob = context.mock(VEGLJob.class);
        final CloudStorageService store = context.mock(CloudStorageService.class);
        final CloudStorageService[] storageServices = {store};
        final VHIRLFileStagingService fileServer = context.mock(VHIRLFileStagingService.class);
        final File activityFile = File.createTempFile("activity", ".ttl");
        URL turtleURL = getClass().getResource("/turtle.ttl");
        final File activityFile2 = new File(turtleURL.toURI());

        vhirlProvenanceService = new VHIRLProvenanceService(fileServer, storageServices);
        vhirlProvenanceService.setServerURL(serverURL);
        VglDownload download = new VglDownload(1);
        download.setUrl("http://portal-uploads.vhirl.org/file1");
        download.setName("file1");
        downloads.add(download);
        CloudFileInformation cloudFileInformation = new CloudFileInformation(cloudKey, 0, "");
        CloudFileInformation cloudFileModel = new CloudFileInformation(activityFileName, 0, "");
        final CloudFileInformation[] cloudList = {cloudFileInformation, cloudFileModel};

        context.checking(new Expectations() {{
            allowing(preparedJob).getId();
            will(returnValue(jobID));
            allowing(preparedJob).getStorageServiceId();
            will(returnValue(cloudServiceID));
            allowing(preparedJob).getJobDownloads();
            will(returnValue(downloads));
            allowing(preparedJob).getName();
            will(returnValue(jobName));
            allowing(preparedJob).getDescription();
            will(returnValue(jobDescription));
            allowing(preparedJob).getProcessDate();
            will(returnValue(new Date()));

            allowing(fileInformation).getCloudKey();
            will(returnValue(cloudKey));

            allowing(fileServer).createLocalFile(activityFileName, preparedJob);
            will(returnValue(activityFile));

            allowing(store).getId();
            will(returnValue(cloudServiceID));
            allowing(store).listJobFiles(preparedJob);
            will(returnValue(cloudList));
            allowing(store).uploadJobFiles(with(any(VEGLJob.class)), with(any(File[].class)));
            allowing(store).getJobFile(preparedJob, activityFileName);
            will(returnValue(new FileInputStream(activityFile2)));
        }});
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateActivity() throws Exception {
        String graph = vhirlProvenanceService.createActivity(preparedJob, serverURL);
        Assert.assertTrue(graph.contains(initalTurtle));
        Assert.assertTrue(graph.contains(intermediateTurtle));
    }

    @Test
    public void testUploadModel() throws Exception {
        vhirlProvenanceService.uploadModel(plainModel, preparedJob);
    }

    @Test
    public void testJobURL() throws Exception {
        String url = VHIRLProvenanceService.jobURL(preparedJob, serverURL);
        Assert.assertEquals(serverURL + "/getJobObject.do?jobId=1", url);
    }

    @Test
    public void testOutputURL() throws Exception {
        String url = VHIRLProvenanceService.outputURL(preparedJob, fileInformation, serverURL);
        Assert.assertEquals(serverURL + "/secure/jobFile.do?jobId=1&key=cloudKey", url);
    }

    @Test
    public void testCreateEntitiesForInputs() throws Exception {
        VHIRLProvenanceService.createEntitiesForInputs(preparedJob);
    }

    @Test
    public void testCreateEntitiesForOutputs() throws Exception {
        String graph = vhirlProvenanceService.createEntitiesForOutputs(preparedJob);
        Assert.assertTrue(graph.contains(initalTurtle));
        Assert.assertTrue(graph.contains(dateAttributionTurtle));
        Assert.assertTrue(graph.contains(file1Turtle));
        Assert.assertTrue(graph.contains(cloudKeyTurtle));
        Assert.assertTrue(graph.contains(generatedTurtle));
    }
}