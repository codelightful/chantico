package org.codelightful.chantico.model;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class ArtifactRequestTest {
    /** Test the scenario when the method parse is invoked with a null URI */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tesParseWithNullUri() {
        ArtifactRequest.parse(null);
    }

    /** Test the scenario when the method parse is invoked with an empty URI */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tesParseWithEmptyUri() {
        ArtifactRequest.parse("");
    }

    /** Test the scenario when a regular versioned file is requested */
    @Test
    public void testParseWithValidArtifactFileUri() {
        ArtifactRequest request = ArtifactRequest.parse("/org/company/package/artifact-name/1.0-VERSION/artifact-file.pom");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.fileName, "artifact-file.pom");
        Assert.assertEquals(request.version, "1.0-VERSION");
        Assert.assertEquals(request.artifact, "artifact-name");
        Assert.assertEquals(request.group, "org.company.package");
        Assert.assertNotNull(request.groupParts);
        Assert.assertEquals(request.groupParts.length, 3);
        Assert.assertEquals(request.groupParts[0], "org");
        Assert.assertEquals(request.groupParts[1], "company");
        Assert.assertEquals(request.groupParts[2], "package");
    }

    /** Test the scenario when a file without version (a package descriptor) URI is received */
    @Test
    public void testParseWithValidArtifactNonVersionedFile() {
        ArtifactRequest request = ArtifactRequest.parse("/org/company/package/artifact-name/maven-metadata.xml");
        Assert.assertNotNull(request);
        Assert.assertEquals(request.fileName, "maven-metadata.xml");
        Assert.assertNull(request.version);
        Assert.assertEquals(request.artifact, "artifact-name");
        Assert.assertEquals(request.group, "org.company.package");
        Assert.assertNotNull(request.groupParts);
        Assert.assertEquals(request.groupParts.length, 3);
        Assert.assertEquals(request.groupParts[0], "org");
        Assert.assertEquals(request.groupParts[1], "company");
        Assert.assertEquals(request.groupParts[2], "package");
    }
}