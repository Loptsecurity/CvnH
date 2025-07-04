package burp.api.montoya.core;

/**
 * Tools in Burp Suite.
 */
public enum ToolType
{
    SUITE("Suite"),
    TARGET("Target"),
    PROXY("Proxy"),
    SCANNER("Scanner"),
    INTRUDER("Intruder"),
    REPEATER("Repeater"),
    LOGGER("Logger"),
    SEQUENCER("Sequencer"),
    DECODER("Decoder"),
    COMPARER("Comparer"),
    EXTENSIONS("Extensions"),
    RECORDED_LOGIN_REPLAYER("Recorded login replayer"),
    ORGANIZER("Organizer");

    private final String toolName;

    ToolType(String toolName)
    {
        this.toolName = toolName;
    }

    /**
     * @return The tool name.
     */
    public String toolName()
    {
        return toolName;
    }

    /**
     * @return The tool name.
     */
    @Override
    public String toString()
    {
        return toolName;
    }
}
