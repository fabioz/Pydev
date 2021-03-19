package com.python.pydev.analysis.flake8;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class Flake8CodesConfigHandler {

    private final static Pattern FLAKE8_CODE_PATTERN = Pattern
            .compile("\\A\\s*([a-zA-Z]+)([0-9]+|(?:\\[\\s*\\-?[0-9]+\\s*,\\s*\\-?[0-9]+\\s*\\])|)\\s*\\Z");

    private final static Map<String, Integer> POSSIBLE_SEVERITIES = initPossibleSeverities();

    public final static Optional<String> checkJsonFormat(JsonValue jsonValue) {
        if (!jsonValue.isObject()) {
            return Optional.of("Flake8 codes config JSON must be an object.");
        }
        JsonObject jsonObject = jsonValue.asObject();

        Map<String, Set<Tuple<Integer, Integer>>> nonOverlappedRanges = new HashMap<String, Set<Tuple<Integer, Integer>>>();

        List<String> codes = jsonObject.names();
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);

            String codeRep = "Code entry " + i;

            Matcher m = FLAKE8_CODE_PATTERN.matcher(code);
            if (!m.matches()) {
                return Optional.of(codeRep
                        + " does not match any of \"prefix\", \"prefixN\" (example: \"prefix123\") or \"prefix[N1,N2]\" patterns.");
            }
            String prefix = m.group(1);
            String identifier = m.group(2);

            Tuple<Integer, Integer> range = getRangeFromIdentifier(identifier);

            int init = range.o1;
            int end = range.o2;

            if (init > end && end != -1) {
                return Optional.of(codeRep + " range init is greater than range end.");
            }

            Set<Tuple<Integer, Integer>> set = nonOverlappedRanges.get(prefix);
            if (set == null) {
                set = new HashSet<Tuple<Integer, Integer>>();
                set.add(range);
                nonOverlappedRanges.put(prefix, set);
            } else {
                Optional<String> overlapErrorMessage = Optional.of(codeRep + " overlap other \"" + prefix + "\" code.");
                Tuple<Integer, Integer> universalTup = new Tuple<Integer, Integer>(-1, -1);
                if (init == -1 && end == -1) {
                    if (set.contains(universalTup)) {
                        return overlapErrorMessage;
                    } else {
                        set.add(universalTup);
                        continue;
                    }
                }
                for (Tuple<Integer, Integer> tup : set) {
                    if (tup.equals(universalTup)) {
                        continue;
                    }
                    if (checkRangeOverlap(range, tup)) {
                        return overlapErrorMessage;
                    }
                }
                set.add(range);
            }

            JsonValue severity = jsonObject.get(code);
            Optional<String> severityError = checkSeverity(codeRep, severity);
            if (!severityError.isEmpty()) {
                return severityError;
            }
        }
        return Optional.empty();
    }

    private static Map<String, Integer> initPossibleSeverities() {
        Map<String, Integer> ret = new HashMap<String, Integer>();
        ret.put("ignore", -1);
        ret.put("error", IMarker.SEVERITY_ERROR);
        ret.put("warning", IMarker.SEVERITY_WARNING);
        ret.put("info", IMarker.SEVERITY_INFO);
        return ret;
    }

    private static Optional<String> checkSeverity(String codeRep, JsonValue severity) {
        if (severity.isNumber()) {
            Collection<Integer> possibleNumbers = POSSIBLE_SEVERITIES.values();
            Optional<String> errorMessage = Optional.of(codeRep + " value does not match any string or "
                    + possibleNumbers.toString() + " integer error values");
            try {
                if (!possibleNumbers.contains(severity.asInt())) {
                    return errorMessage;
                }
            } catch (Exception e) {
                return errorMessage;
            }
        } else if (severity.isString()) {
            Set<String> possibleStrings = POSSIBLE_SEVERITIES.keySet();
            String severityStr = severity.asString().trim();
            if (!possibleStrings.contains(severityStr)) {
                return Optional.of(codeRep + " value does not match any integer or "
                        + possibleStrings.toString() + " string error values");
            }
        } else {
            return Optional.of(codeRep + " value is neither a string or an integer");
        }
        return Optional.empty();
    }

    public static boolean checkRangeOverlap(Tuple<Integer, Integer> compareRange, Tuple<Integer, Integer> baseRange) {
        int c1 = compareRange.o1;
        int c2 = compareRange.o2;
        int b1 = baseRange.o1;
        int b2 = baseRange.o2;

        if (b1 == c1 || b2 == c2) {
            return true;
        }
        if ((c1 <= b2 || b2 == -1) && (b1 <= c2 || c2 == -1)) {
            return true;
        }
        return false;
    }

    private final static Tuple<Integer, Integer> getRangeFromIdentifier(String identifier) {
        if (identifier.contains("[")) {
            String valuesStr = identifier.substring(1, identifier.length() - 1);
            List<String> values = StringUtils.split(valuesStr, ',');
            int firstValue = Integer.parseInt(values.get(0).trim());
            int secondValue = Integer.parseInt(values.get(1).trim());
            return new Tuple<Integer, Integer>(firstValue, secondValue);
        } else if (identifier.isBlank()) {
            return new Tuple<Integer, Integer>(-1, -1);
        } else {
            int value = Integer.parseInt(identifier);
            return new Tuple<Integer, Integer>(value, value);
        }
    }

    public static Map<String, Tuple<Set<Tuple<Integer, Integer>>, Integer>> getCodeSeveritiesFromConfig(
            IAdaptable projectAdaptable) {
        Map<String, Tuple<Set<Tuple<Integer, Integer>>, Integer>> ret = new HashMap<String, Tuple<Set<Tuple<Integer, Integer>>, Integer>>();
        String str = Flake8Preferences.getCodesConfig(projectAdaptable);
        JsonObject jsonObject = JsonObject.readFrom(str);

        for (String code : jsonObject.names()) {
            Tuple<String, Tuple<Integer, Integer>> codeTup = getCodeTuple(code);
            String prefix = codeTup.o1;
            Tuple<Integer, Integer> range = codeTup.o2;

            int severity = getSeverity(jsonObject.get(code));
            Tuple<Set<Tuple<Integer, Integer>>, Integer> valueTup = ret.get(prefix);
            if (valueTup == null) {
                Set<Tuple<Integer, Integer>> set = new HashSet<Tuple<Integer, Integer>>();
                set.add(range);
                ret.put(prefix, new Tuple<Set<Tuple<Integer, Integer>>, Integer>(set, severity));
            } else {
                Set<Tuple<Integer, Integer>> set = valueTup.o1;
                set.add(range);
            }
        }
        return ret;
    }

    private static int getSeverity(JsonValue jsonValue) {
        if (jsonValue.isNumber()) {
            return jsonValue.asInt();
        } else {
            return POSSIBLE_SEVERITIES.get(jsonValue.asString().trim());
        }
    }

    public static Tuple<String, Tuple<Integer, Integer>> getCodeTuple(String code) {
        int prefixEnd = code.length();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (!Character.isAlphabetic(c)) {
                prefixEnd = i;
                break;
            }
        }
        String prefix = code.substring(0, prefixEnd);
        String identifier = code.substring(prefixEnd);
        Tuple<Integer, Integer> range = getRangeFromIdentifier(identifier);
        return new Tuple<String, Tuple<Integer, Integer>>(prefix, range);
    }
}
