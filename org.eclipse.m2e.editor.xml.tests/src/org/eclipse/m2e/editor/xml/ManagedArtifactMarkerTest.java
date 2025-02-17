/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Assert;
import org.junit.Test;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: everytime you do mindless test update
 * add an 'y' here: y
 * 
 * @author mkleint
 */

public class ManagedArtifactMarkerTest extends AbstractMavenProjectTestCase {
	 @Test
  public void testMNGEclipse2559() throws Exception {
    ResolverConfiguration config = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MNGECLIPSE-2559", new String[] {"pom.xml"}, config);
    waitForJobsToComplete();

    IProject project = projects[0];
    IMarker[] markers = XmlEditorHelpers.findEditorHintWarningMarkers(project).toArray(new IMarker[0]);
    assertEquals(2, markers.length);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, null /* message */, 18 /* lineNumber */,
        3 /* resolutions */, markers[0]);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, null /* message */, 47 /* lineNumber */,
        3 /* resolutions */, markers[1]);

    assertEquals("org.apache.maven.plugins", markers[1].getAttribute("groupId"));
    assertEquals("maven-compiler-plugin", markers[1].getAttribute("artifactId"));
    // not defined in profile
    assertEquals(null, markers[1].getAttribute("profile"));

    assertEquals("ant", markers[0].getAttribute("groupId"));
    assertEquals("ant-apache-oro", markers[0].getAttribute("artifactId"));
    // not defined in profile
    assertEquals(null, markers[0].getAttribute("profile"));

    // Fix the problem - the marker should be removed
    copyContent(project, "pom_good.xml", "pom.xml");
    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
  }

  // splitted the test in two as both projects failed to load together!!!! why? shall I bother?
  @Test 
  public void testMNGEclipse2559Second() throws Exception {
    ResolverConfiguration config = new ResolverConfiguration();
    config.setSelectedProfiles("plug,depend");
    IProject[] projects = importProjects("projects/MNGECLIPSE-2559", new String[] {"withProfileActivated/pom.xml"},
        config);
    waitForJobsToComplete();

    IProject project = projects[0];
    IMarker[] markers = XmlEditorHelpers.findEditorHintWarningMarkers(project).toArray(new IMarker[0]);
    assertEquals(2, markers.length);

    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, null /* message */, 21 /* lineNumber */,
        3 /* resolutions */, markers[0]);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, null /* message */, 41 /* lineNumber */,
        3 /* resolutions */, markers[1]);

    assertEquals("org.apache.maven.plugins", markers[1].getAttribute("groupId"));
    assertEquals("maven-compiler-plugin", markers[1].getAttribute("artifactId"));
    assertEquals("plug", markers[1].getAttribute("profile"));

    assertEquals("ant", markers[0].getAttribute("groupId"));
    assertEquals("ant-apache-oro", markers[0].getAttribute("artifactId"));
    assertEquals("depend", markers[0].getAttribute("profile"));

    // Fix the problem - the marker should be removed
    copyContent(project, "pom_good.xml", "pom.xml");
    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
  }
  @Test
  public void test355882_noMarkerOnDifferentClassifier() throws Exception {
	    IProject project = importProject("projects/355882/test1/pom.xml");
	    waitForJobsToComplete();
	    assertTrue(Stream.of(project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) //
	    	.map(marker -> marker.getAttribute(IMarker.MESSAGE, "")) //
	    	.noneMatch(message -> message.contains("Overriding managed version"))); //$NON-NLS-1$
  }
  @Test
  public void test355882_MarkerOnClassifierVersionOverride() throws Exception {
	    IProject project = importProject("projects/355882/test2/pom.xml");
	    waitForJobsToComplete();
	    
	    IMarker[] markers = project.findMarkers(IMavenConstants.MARKER_POM_LOADING_ID, true, IResource.DEPTH_INFINITE);
	    assertEquals(1, markers.length);

	    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
	        IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, null /* message */, 12 /* lineNumber */,
	        3 /* resolutions */, markers[0]);

  }
  
  @Test
  public void test439309_errorOnManagedVersionOverride() throws Exception {
    String originalSeverity = mavenConfiguration.getOverridingManagedVersionExecutionSeverity();
    try {
          ((MavenConfigurationImpl) mavenConfiguration).setOverridingManagedVersionExecutionSeverity(ProblemSeverity.error
             .toString());
    
    IProject project = importProject("projects/MNGECLIPSE-2559/pom.xml");
    waitForJobsToComplete();

    List<IMarker> markers = XmlEditorHelpers.findErrorMarkers(project);
    assertEquals(2, markers.size());
    XmlEditorHelpers.assertEditorHintErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, null /* message */, 18 /* lineNumber */,
        3 /* resolutions */, markers.get(0));
    XmlEditorHelpers.assertEditorHintErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, null /* message */, 47 /* lineNumber */,
        3 /* resolutions */, markers.get(1));
	verifyOpenManagedResolution(markers.get(0), "1.6.3");
	verifyOpenManagedResolution(markers.get(1), "2.3.2");

    } finally {
       ((MavenConfigurationImpl) mavenConfiguration).setOverridingManagedVersionExecutionSeverity(originalSeverity);
    }
  }

	private void verifyOpenManagedResolution(IMarker marker, String expected) throws BadLocationException {
		IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
		for (IMarkerResolution resolution : resolutions) {
			if (resolution instanceof org.eclipse.m2e.editor.internal.markers.OpenManagedVersionDefinitionResolution) {
				resolution.run(marker);
				IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.getActiveEditor();
				assertTrue(editor instanceof MavenPomEditor);
				ITextEditor sourcePage = ((MavenPomEditor) editor).getSourcePage();
				TextSelection selection = (TextSelection) sourcePage.getSelectionProvider().getSelection();
				String string = sourcePage.getDocumentProvider().getDocument(sourcePage.getEditorInput())
						.get(selection.getOffset(), expected.length());
				assertEquals(expected, string);
				((MavenPomEditor) editor).close(false);
				return;
			}
		}
		Assert.fail("OpenManagedVersionDefinitionResolution not found");
	}
	
  @Test
  public void test439309_ignoreManagedVersionOverride() throws Exception {
    String originalSeverity = mavenConfiguration.getOverridingManagedVersionExecutionSeverity();
    try {
          ((MavenConfigurationImpl) mavenConfiguration).setOverridingManagedVersionExecutionSeverity(ProblemSeverity.ignore
             .toString());
    
    IProject project = importProject("projects/MNGECLIPSE-2559/pom.xml");
    waitForJobsToComplete();

    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
    XmlEditorHelpers.assertNoErrors(project);

    } finally {
       ((MavenConfigurationImpl) mavenConfiguration).setOverridingManagedVersionExecutionSeverity(originalSeverity);
    }
  }

  @Test
  public void test553839_noErrorCausedByPluginVersionFromSuperPom() throws Exception {
    IProject project = importProject("projects/553839/pom.xml");
    waitForJobsToComplete();

    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
    XmlEditorHelpers.assertNoErrors(project);
  }

}
