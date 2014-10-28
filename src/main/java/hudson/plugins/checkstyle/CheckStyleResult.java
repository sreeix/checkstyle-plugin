package hudson.plugins.checkstyle;

import com.thoughtworks.xstream.XStream;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.analysis.core.BuildHistory;
import hudson.plugins.analysis.core.BuildResult;
import hudson.plugins.analysis.core.ParserResult;
import hudson.plugins.analysis.core.ResultAction;
import hudson.plugins.checkstyle.parser.Warning;

/**
 * Represents the results of the Checkstyle analysis. One instance of this class
 * is persisted for each build via an XML file.
 *
 * @author Ulli Hafner
 */
public class CheckStyleResult extends BuildResult {
    private static final long serialVersionUID = 2768250056765266658L;
    private boolean failedBecauseOfRatchet = false;

    /**
     * Creates a new instance of {@link CheckStyleResult}.
     *
     * @param build
     *            the current build as owner of this action
     * @param defaultEncoding
     *            the default encoding to be used when reading and parsing files
     * @param result
     *            the parsed result with all annotations
     * @param useStableBuildAsReference
     *            determines whether only stable builds should be used as
     *            reference builds or not
     */
    public CheckStyleResult(final AbstractBuild<?, ?> build, final String defaultEncoding, final ParserResult result,
            final boolean useStableBuildAsReference, final boolean shouldRatchet) {
        this(build, defaultEncoding, result, useStableBuildAsReference, CheckStyleResultAction.class, shouldRatchet);
    }

    /**
     * Creates a new instance of {@link CheckStyleResult}.
     *
     * @param build
     *            the current build as owner of this action
     * @param defaultEncoding
     *            the default encoding to be used when reading and parsing files
     * @param result
     *            the parsed result with all annotations
     * @param useStableBuildAsReference
     *            determines whether only stable builds should be used as
     *            reference builds or not
     * @param actionType
     *            the type of the result action
     */
    protected CheckStyleResult(final AbstractBuild<?, ?> build, final String defaultEncoding, final ParserResult result,
            final boolean useStableBuildAsReference, final Class<? extends ResultAction<CheckStyleResult>> actionType, final boolean shouldRatchet) {
        this(build, new BuildHistory(build, actionType, useStableBuildAsReference), result, defaultEncoding, true, shouldRatchet);
    }

    CheckStyleResult(final AbstractBuild<?, ?> build, final BuildHistory history,
            final ParserResult result, final String defaultEncoding, final boolean canSerialize, final boolean shouldRatchet) {
        super(build, history, result, defaultEncoding);

        if (canSerialize) {
            serializeAnnotations(result.getAnnotations());
        }
        if(shouldRatchet && this.isSuccessful() && hasPreviousWarningHistory() && this.getNumberOfNewWarnings() > 0) {
            this.failedBecauseOfRatchet = true;
            this.setResult(Result.FAILURE);
        }
    }

    public boolean hasRatchetFailed() {
        return this.failedBecauseOfRatchet;
    }

    private boolean hasPreviousWarningHistory() {
        return this.getHistory().hasReferenceBuild() && this.getHistory().getPreviousResult().getNumberOfWarnings() > 0;
    }

    @Override
    public String getHeader() {
        return Messages.Checkstyle_ResultAction_Header();
    }

    @Override
    protected void configure(final XStream xstream) {
        xstream.alias("warning", Warning.class);
    }

    @Override
    public String getSummary() {
        return "Checkstyle: " + createDefaultSummary(CheckStyleDescriptor.RESULT_URL, getNumberOfAnnotations(), getNumberOfModules());
    }

    @Override
    protected String createDeltaMessage() {
        return createDefaultDeltaMessage(CheckStyleDescriptor.RESULT_URL, getNumberOfNewWarnings(), getNumberOfFixedWarnings());
    }

    /**
     * Returns the name of the file to store the serialized annotations.
     *
     * @return the name of the file to store the serialized annotations
     */
    @Override
    protected String getSerializationFileName() {
        return "checkstyle-warnings.xml";
    }

    @Override
    public String getDisplayName() {
        return Messages.Checkstyle_ProjectAction_Name();
    }

    @Override
    protected Class<? extends ResultAction<? extends BuildResult>> getResultActionType() {
        return CheckStyleResultAction.class;
    }
}
