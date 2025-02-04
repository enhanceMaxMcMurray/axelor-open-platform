/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.gradle.util;

import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import javax.annotation.Nullable;

/**
 * Represents, parses, and compares version numbers. Supports a couple of different schemes:
 *
 * <ul>
 *   <li>MAJOR.MINOR.MICRO-QUALIFIER (the default).
 *   <li>MAJOR.MINOR.MICRO.PATCH-QUALIFIER.
 * </ul>
 *
 * <p>The {@link #parse} method handles missing parts and allows "." to be used instead of "-", and
 * "_" to be used instead of "." for the patch number.
 *
 * <p>This class considers missing parts to be 0, so that "1.0" == "1.0.0" == "1.0.0_0".
 *
 * <p>Note that this class considers "1.2.3-something" less than "1.2.3". Qualifiers are compared
 * lexicographically ("1.2.3-alpha" < "1.2.3-beta") and case-insensitive ("1.2.3-alpha" <
 * "1.2.3.RELEASE").
 *
 * <p>To check if a version number is at least "1.2.3", disregarding a potential qualifier like
 * "beta", use {@code version.getBaseVersion().compareTo(VersionNumber.parse("1.2.3")) >= 0}.
 */
public class VersionNumber implements Comparable<VersionNumber> {
  private static final DefaultScheme DEFAULT_SCHEME = new DefaultScheme();
  private static final SchemeWithPatchVersion PATCH_SCHEME = new SchemeWithPatchVersion();
  public static final VersionNumber UNKNOWN = version(0);

  private final int major;
  private final int minor;
  private final int micro;
  private final int patch;
  private final String qualifier;
  private final AbstractScheme scheme;

  public VersionNumber(int major, int minor, int micro, @Nullable String qualifier) {
    this(major, minor, micro, 0, qualifier, DEFAULT_SCHEME);
  }

  public VersionNumber(int major, int minor, int micro, int patch, @Nullable String qualifier) {
    this(major, minor, micro, patch, qualifier, PATCH_SCHEME);
  }

  private VersionNumber(
      int major,
      int minor,
      int micro,
      int patch,
      @Nullable String qualifier,
      AbstractScheme scheme) {
    this.major = major;
    this.minor = minor;
    this.micro = micro;
    this.patch = patch;
    this.qualifier = qualifier;
    this.scheme = scheme;
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getMicro() {
    return micro;
  }

  public int getPatch() {
    return patch;
  }

  @Nullable
  public String getQualifier() {
    return qualifier;
  }

  public VersionNumber getBaseVersion() {
    return new VersionNumber(major, minor, micro, patch, null, scheme);
  }

  @Override
  public int compareTo(VersionNumber other) {
    if (major != other.major) {
      return major - other.major;
    }
    if (minor != other.minor) {
      return minor - other.minor;
    }
    if (micro != other.micro) {
      return micro - other.micro;
    }
    if (patch != other.patch) {
      return patch - other.patch;
    }
    return Ordering.natural()
        .nullsLast()
        .compare(toLowerCase(qualifier), toLowerCase(other.qualifier));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof VersionNumber && compareTo((VersionNumber) other) == 0;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + micro;
    result = 31 * result + patch;
    result = 31 * result + Objects.hashCode(qualifier);
    return result;
  }

  @Override
  public String toString() {
    return scheme.format(this);
  }

  public static VersionNumber version(int major) {
    return version(major, 0);
  }

  public static VersionNumber version(int major, int minor) {
    return new VersionNumber(major, minor, 0, 0, null, DEFAULT_SCHEME);
  }

  /** Returns the default MAJOR.MINOR.MICRO-QUALIFIER scheme. */
  public static Scheme scheme() {
    return DEFAULT_SCHEME;
  }

  /** Returns the MAJOR.MINOR.MICRO.PATCH-QUALIFIER scheme. */
  public static Scheme withPatchNumber() {
    return PATCH_SCHEME;
  }

  public static VersionNumber parse(String versionString) {
    return DEFAULT_SCHEME.parse(versionString);
  }

  @Nullable
  private String toLowerCase(@Nullable String string) {
    return string == null ? null : string.toLowerCase();
  }

  public interface Scheme {
    VersionNumber parse(String value);

    String format(VersionNumber versionNumber);
  }

  private abstract static class AbstractScheme implements Scheme {
    final int depth;

    protected AbstractScheme(int depth) {
      this.depth = depth;
    }

    @Override
    public VersionNumber parse(@Nullable String versionString) {
      if (versionString == null || versionString.length() == 0) {
        return UNKNOWN;
      }
      Scanner scanner = new Scanner(versionString);

      int major = 0;
      int minor = 0;
      int micro = 0;
      int patch = 0;

      if (!scanner.hasDigit()) {
        return UNKNOWN;
      }
      major = scanner.scanDigit();
      if (scanner.isSeparatorAndDigit('.')) {
        scanner.skipSeparator();
        minor = scanner.scanDigit();
        if (scanner.isSeparatorAndDigit('.')) {
          scanner.skipSeparator();
          micro = scanner.scanDigit();
          if (depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
            scanner.skipSeparator();
            patch = scanner.scanDigit();
          }
        }
      }

      if (scanner.isEnd()) {
        return new VersionNumber(major, minor, micro, patch, null, this);
      }

      if (scanner.isQualifier()) {
        scanner.skipSeparator();
        return new VersionNumber(major, minor, micro, patch, scanner.remainder(), this);
      }

      return UNKNOWN;
    }

    private static class Scanner {
      int pos;
      final String str;

      private Scanner(String string) {
        this.str = string;
      }

      boolean hasDigit() {
        return pos < str.length() && Character.isDigit(str.charAt(pos));
      }

      boolean isSeparatorAndDigit(char... separators) {
        return pos < str.length() - 1
            && oneOf(separators)
            && Character.isDigit(str.charAt(pos + 1));
      }

      private boolean oneOf(char... separators) {
        char current = str.charAt(pos);
        for (int i = 0; i < separators.length; i++) {
          char separator = separators[i];
          if (current == separator) {
            return true;
          }
        }
        return false;
      }

      boolean isQualifier() {
        return pos < str.length() - 1 && oneOf('.', '-');
      }

      int scanDigit() {
        int start = pos;
        while (hasDigit()) {
          pos++;
        }
        return Integer.parseInt(str.substring(start, pos));
      }

      public boolean isEnd() {
        return pos == str.length();
      }

      public void skipSeparator() {
        pos++;
      }

      @Nullable
      public String remainder() {
        return pos == str.length() ? null : str.substring(pos);
      }
    }
  }

  private static class DefaultScheme extends AbstractScheme {
    private static final String VERSION_TEMPLATE = "%d.%d.%d%s";

    public DefaultScheme() {
      super(3);
    }

    @Override
    public String format(VersionNumber versionNumber) {
      return String.format(
          VERSION_TEMPLATE,
          versionNumber.major,
          versionNumber.minor,
          versionNumber.micro,
          versionNumber.qualifier == null ? "" : "-" + versionNumber.qualifier);
    }
  }

  private static class SchemeWithPatchVersion extends AbstractScheme {
    private static final String VERSION_TEMPLATE = "%d.%d.%d.%d%s";

    private SchemeWithPatchVersion() {
      super(4);
    }

    @Override
    public String format(VersionNumber versionNumber) {
      return String.format(
          VERSION_TEMPLATE,
          versionNumber.major,
          versionNumber.minor,
          versionNumber.micro,
          versionNumber.patch,
          versionNumber.qualifier == null ? "" : "-" + versionNumber.qualifier);
    }
  }
}
