package org.python.pydev.shared_core.version;

import java.util.StringTokenizer;

public class Version implements Comparable<Version> {
    private int major;
    private int minor;
    private int patch;

    public Version(String versionString) {
        StringTokenizer tokenizer = new StringTokenizer(versionString, ".");
        if (tokenizer.hasMoreTokens()) {
            major = Integer.parseInt(tokenizer.nextToken());
        }
        if (tokenizer.hasMoreTokens()) {
            minor = Integer.parseInt(tokenizer.nextToken());
        }
        if (tokenizer.hasMoreTokens()) {
            patch = Integer.parseInt(tokenizer.nextToken());
        }
    }

    @Override
    public int compareTo(Version other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    public boolean isGreaterThanOrEqualTo(Version other) {
        return compareTo(other) >= 0;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}