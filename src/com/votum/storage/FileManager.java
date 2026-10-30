package com.votum.storage;

import com.votum.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Manages file-based storage for all Votum entities.
 * Uses human-readable delimiter-separated text files (CSV/Pipe format).
 * Automatically initializes demo data if files do not exist.
 */
public final class FileManager {
    private static final String DELIMITER = ";";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String dataDir;
    private final String usersFile;
    private final String votersFile;
    private final String candidatesFile;
    private final String electionsFile;
    private final String votesFile;
    private final String auditSecurityFile;

    public FileManager() {
        this("data");
    }

    public FileManager(String dataDir) {
        this.dataDir = dataDir;
        this.usersFile = dataDir + File.separator + "users.txt";
        this.votersFile = dataDir + File.separator + "voters.txt";
        this.candidatesFile = dataDir + File.separator + "candidates.txt";
        this.electionsFile = dataDir + File.separator + "elections.txt";
        this.votesFile = dataDir + File.separator + "votes.txt";
        this.auditSecurityFile = dataDir + File.separator + "security.txt";

        ensureDirectoryExists();
        initializeDemoDataIfEmpty();
    }

    private void ensureDirectoryExists() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Seeds initial demo data if the data directory is fresh.
     * Private so that the constructor cannot leak a partially initialised instance.
     */
    private void initializeDemoDataIfEmpty() {
        File uFile = new File(usersFile);
        if (!uFile.exists() || uFile.length() == 0) {
            List<Admin> defaultAdmins = new ArrayList<>();
            defaultAdmins.add(new Admin("admin", "admin123", "ADM001", "Campus Election Officer", "admin@votum.edu"));
            saveAdmins(defaultAdmins);
        }

        File eFile = new File(electionsFile);
        if (!eFile.exists() || eFile.length() == 0) {
            List<Election> defaultElections = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            // Default active window: from 1 hour ago until tomorrow
            defaultElections.add(new Election(
                    "E001",
                    "Student Council President 2026",
                    "Annual general election for student body president",
                    now.minusHours(1),
                    now.plusDays(1),
                    ElectionStatus.OPEN));
            saveElections(defaultElections);
        }

        File cFile = new File(candidatesFile);
        if (!cFile.exists() || cFile.length() == 0) {
            List<Candidate> defaultCandidates = new ArrayList<>();
            defaultCandidates.add(new Candidate("C001", "Arjun Sharma", "Computer Science", "E001",
                    "Better Campus Wi-Fi & Tech Labs"));
            defaultCandidates.add(new Candidate("C002", "Priya Mehta", "Information Technology", "E001",
                    "Transparent Student Representation"));
            defaultCandidates.add(new Candidate("C003", "Rahul Verma", "Electronics & Comm.", "E001",
                    "Sports & Cultural Fest Upgrades"));
            saveCandidates(defaultCandidates);
        }

        File vFile = new File(votersFile);
        if (!vFile.exists() || vFile.length() == 0) {
            List<Voter> defaultVoters = new ArrayList<>();
            defaultVoters.add(new Voter("V001", "Aarav Gupta", "Computer Science", "voter1", "voter123", true, false));
            defaultVoters.add(
                    new Voter("V002", "Ananya Singh", "Information Technology", "voter2", "voter123", true, false));
            defaultVoters.add(new Voter("V003", "Rohan Nair", "Mechanical Engg", "voter3", "voter123", true, false));
            defaultVoters
                    .add(new Voter("V004", "Sneha Patel", "Electronics & Comm.", "voter4", "voter123", true, false));
            defaultVoters.add(new Voter("V005", "Dev Sharma", "Computer Science", "voter5", "voter123", false, false)); // Ineligible
                                                                                                                        // demo
            saveVoters(defaultVoters);
        }

        File vtFile = new File(votesFile);
        if (!vtFile.exists()) {
            try {
                vtFile.createNewFile();
            } catch (IOException ignored) {
                assert ignored != null;
                /* ignore */
            }
        }
    }

    /**
     * Explicitly resets the persistent data store to the documented official demo
     * baseline.
     * Restores default administrator, default election (with active window),
     * default candidates,
     * default eligible/ineligible voters, and clears all cast ballots.
     *
     * @return true if all files were successfully restored, false otherwise
     */
    public synchronized boolean resetToDemoData() {
        List<Admin> defaultAdmins = new ArrayList<>();
        defaultAdmins.add(new Admin("admin", "admin123", "ADM001", "Campus Election Officer", "admin@votum.edu"));

        List<Election> defaultElections = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        defaultElections.add(new Election(
                "E001",
                "Student Council President 2026",
                "Annual general election for student body president",
                now.minusHours(1),
                now.plusDays(7),
                ElectionStatus.OPEN));

        List<Candidate> defaultCandidates = new ArrayList<>();
        defaultCandidates.add(
                new Candidate("C001", "Arjun Sharma", "Computer Science", "E001", "Better Campus Wi-Fi & Tech Labs"));
        defaultCandidates.add(new Candidate("C002", "Priya Mehta", "Information Technology", "E001",
                "Transparent Student Representation"));
        defaultCandidates.add(
                new Candidate("C003", "Rahul Verma", "Electronics & Comm.", "E001", "Sports & Cultural Fest Upgrades"));

        List<Voter> defaultVoters = new ArrayList<>();
        defaultVoters.add(new Voter("V001", "Aarav Gupta", "Computer Science", "voter1", "voter123", true, false, ""));
        defaultVoters.add(
                new Voter("V002", "Ananya Singh", "Information Technology", "voter2", "voter123", true, false, ""));
        defaultVoters.add(new Voter("V003", "Rohan Nair", "Mechanical Engg", "voter3", "voter123", true, false, ""));
        defaultVoters
                .add(new Voter("V004", "Sneha Patel", "Electronics & Comm.", "voter4", "voter123", true, false, ""));
        defaultVoters.add(new Voter("V005", "Dev Sharma", "Computer Science", "voter5", "voter123", false, false, ""));

        boolean ok = saveAdmins(defaultAdmins);
        ok = ok && saveElections(defaultElections);
        ok = ok && saveCandidates(defaultCandidates);
        ok = ok && saveVoters(defaultVoters);
        ok = ok && saveVotes(new ArrayList<>());
        return ok;
    }

    // ==========================================
    // ADMINS
    // ==========================================
    public synchronized List<Admin> loadAdmins() {
        List<Admin> admins = new ArrayList<>();
        File file = new File(usersFile);
        if (!file.exists())
            return admins;

        int skipped = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 5) {
                    skipped++;
                    warnMalformedRecord("users.txt", line, "expected 5 columns, found " + parts.length);
                    continue;
                }
                admins.add(new Admin(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
        } catch (IOException e) {
            System.err.println("Error loading admins: " + e.getMessage());
        }
        if (skipped > 0) {
            System.err.println("Warning: " + skipped + " malformed record(s) skipped in " + usersFile);
        }
        return admins;
    }

    public synchronized boolean saveAdmins(List<Admin> admins) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(usersFile))) {
            for (Admin admin : admins) {
                String line = String.join(DELIMITER,
                        sanitize(admin.getUsername()),
                        sanitize(admin.getPassword()),
                        sanitize(admin.getAdminId()),
                        sanitize(admin.getFullName()),
                        sanitize(admin.getEmail()));
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error saving admins: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // VOTERS
    // ==========================================
    public synchronized List<Voter> loadVoters() {
        List<Voter> voters = new ArrayList<>();
        File file = new File(votersFile);
        if (!file.exists())
            return voters;

        int skipped = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 7) {
                    skipped++;
                    warnMalformedRecord("voters.txt", line, "expected at least 7 columns, found " + parts.length);
                    continue;
                }
                String voterId = parts[0];
                String name = parts[1];
                String department = parts[2];
                String username = parts[3];
                String password = parts[4];
                boolean eligible = Boolean.parseBoolean(parts[5]);
                boolean hasVoted = Boolean.parseBoolean(parts[6]);
                String votedElectionId = (parts.length >= 8) ? parts[7] : "";
                voters.add(
                        new Voter(voterId, name, department, username, password, eligible, hasVoted, votedElectionId));
            }
        } catch (IOException e) {
            System.err.println("Error loading voters: " + e.getMessage());
        }
        if (skipped > 0) {
            System.err.println("Warning: " + skipped + " malformed record(s) skipped in " + votersFile);
        }
        return voters;
    }

    public synchronized boolean saveVoters(List<Voter> voters) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(votersFile))) {
            for (Voter voter : voters) {
                String line = String.join(DELIMITER,
                        sanitize(voter.getVoterId()),
                        sanitize(voter.getName()),
                        sanitize(voter.getDepartment()),
                        sanitize(voter.getUsername()),
                        sanitize(voter.getPassword()),
                        String.valueOf(voter.isEligible()),
                        String.valueOf(voter.hasVoted()),
                        sanitize(voter.getVotedElectionId()));
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error saving voters: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateVoter(Voter updatedVoter) {
        List<Voter> voters = loadVoters();
        boolean found = false;
        for (int i = 0; i < voters.size(); i++) {
            if (voters.get(i).getVoterId().equalsIgnoreCase(updatedVoter.getVoterId())) {
                voters.set(i, updatedVoter);
                found = true;
                break;
            }
        }
        if (!found) {
            voters.add(updatedVoter);
        }
        return saveVoters(voters);
    }

    public synchronized boolean setVoterEligibility(String voterId, boolean eligible) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return false;
        }
        List<Voter> voters = loadVoters();
        boolean found = false;
        for (Voter v : voters) {
            if (v.getVoterId().equalsIgnoreCase(voterId.trim())) {
                v.setEligible(eligible);
                found = true;
                break;
            }
        }
        if (found) {
            saveVoters(voters);
        }
        return found;
    }

    public synchronized boolean deleteVoter(String voterId) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return false;
        }
        List<Voter> voters = loadVoters();
        boolean removed = voters.removeIf(v -> v.getVoterId().equalsIgnoreCase(voterId.trim()));
        if (removed) {
            saveVoters(voters);
            // Cascade: remove votes associated with this voter
            List<Vote> votes = loadVotes();
            boolean votesChanged = votes.removeIf(v -> v.getVoterId().equalsIgnoreCase(voterId.trim()));
            if (votesChanged) {
                saveVotes(votes);
            }
        }
        return removed;
    }

    // ==========================================
    // CANDIDATES
    // ==========================================
    public synchronized List<Candidate> loadCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        File file = new File(candidatesFile);
        if (!file.exists())
            return candidates;

        int skipped = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 4) {
                    skipped++;
                    warnMalformedRecord("candidates.txt", line, "expected at least 4 columns, found " + parts.length);
                    continue;
                }
                String candidateId = parts[0];
                String name = parts[1];
                String department = parts[2];
                String electionId = parts[3];
                String slogan = (parts.length >= 5) ? parts[4] : "";
                candidates.add(new Candidate(candidateId, name, department, electionId, slogan));
            }
        } catch (IOException e) {
            System.err.println("Error loading candidates: " + e.getMessage());
        }
        if (skipped > 0) {
            System.err.println("Warning: " + skipped + " malformed record(s) skipped in " + candidatesFile);
        }
        return candidates;
    }

    public synchronized boolean saveCandidates(List<Candidate> candidates) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(candidatesFile))) {
            for (Candidate candidate : candidates) {
                String line = String.join(DELIMITER,
                        sanitize(candidate.getCandidateId()),
                        sanitize(candidate.getName()),
                        sanitize(candidate.getDepartment()),
                        sanitize(candidate.getElectionId()),
                        sanitize(candidate.getSlogan()));
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error saving candidates: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean addCandidate(Candidate candidate) {
        List<Candidate> candidates = loadCandidates();
        candidates.add(candidate);
        return saveCandidates(candidates);
    }

    public synchronized boolean updateCandidate(Candidate candidate) {
        if (candidate == null || candidate.getCandidateId().trim().isEmpty()) {
            return false;
        }
        List<Candidate> candidates = loadCandidates();
        boolean found = false;
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getCandidateId().equalsIgnoreCase(candidate.getCandidateId().trim())) {
                candidates.set(i, candidate);
                found = true;
                break;
            }
        }
        if (!found) {
            candidates.add(candidate);
        }
        return saveCandidates(candidates);
    }

    public synchronized boolean removeCandidate(String candidateId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            return false;
        }
        List<Candidate> candidates = loadCandidates();
        boolean removed = candidates.removeIf(c -> c.getCandidateId().equalsIgnoreCase(candidateId.trim()));
        if (removed) {
            saveCandidates(candidates);
            // Cascade: remove votes associated with this candidate and reset affected voters
            List<Vote> votes = loadVotes();
            Set<String> affectedVoterIds = new HashSet<>();
            for (Vote v : votes) {
                if (v.getCandidateId().equalsIgnoreCase(candidateId.trim())) {
                    affectedVoterIds.add(v.getVoterId());
                }
            }
            if (!affectedVoterIds.isEmpty()) {
                votes.removeIf(v -> v.getCandidateId().equalsIgnoreCase(candidateId.trim()));
                saveVotes(votes);
                List<Voter> voters = loadVoters();
                for (Voter v : voters) {
                    if (affectedVoterIds.contains(v.getVoterId())) {
                        v.setHasVoted(false);
                        v.setVotedElectionId("");
                    }
                }
                saveVoters(voters);
            }
        }
        return removed;
    }

    // ==========================================
    // ELECTIONS
    // ==========================================
    public synchronized List<Election> loadElections() {
        List<Election> elections = new ArrayList<>();
        File file = new File(electionsFile);
        if (!file.exists())
            return elections;

        int skipped = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 6) {
                    skipped++;
                    warnMalformedRecord("elections.txt", line, "expected 6 columns, found " + parts.length);
                    continue;
                }
                LocalDateTime startTime = parseDateTime(parts[3]);
                LocalDateTime endTime = parseDateTime(parts[4]);
                ElectionStatus status = parseStatus(parts[5]);
                if (startTime == null || endTime == null || status == null) {
                    skipped++;
                    warnMalformedRecord("elections.txt", line, "unparseable start time, end time or status");
                    continue;
                }
                elections.add(new Election(parts[0], parts[1], parts[2], startTime, endTime, status));
            }
        } catch (IOException e) {
            System.err.println("Error loading elections: " + e.getMessage());
        }
        if (skipped > 0) {
            System.err.println("Warning: " + skipped + " malformed record(s) skipped in " + electionsFile);
        }
        return elections;
    }

    public synchronized boolean saveElections(List<Election> elections) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(electionsFile))) {
            for (Election election : elections) {
                String startStr = election.getStartTime() != null ? election.getStartTime().format(DATE_TIME_FORMATTER)
                        : "";
                String endStr = election.getEndTime() != null ? election.getEndTime().format(DATE_TIME_FORMATTER) : "";
                String line = String.join(DELIMITER,
                        sanitize(election.getElectionId()),
                        sanitize(election.getName()),
                        sanitize(election.getDescription()),
                        startStr,
                        endStr,
                        election.getStatus().name());
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error saving elections: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean saveOrUpdateElection(Election election) {
        List<Election> elections = loadElections();
        boolean found = false;
        for (int i = 0; i < elections.size(); i++) {
            if (elections.get(i).getElectionId().equalsIgnoreCase(election.getElectionId())) {
                elections.set(i, election);
                found = true;
                break;
            }
        }
        if (!found) {
            elections.add(election);
        }
        return saveElections(elections);
    }

    public synchronized boolean deleteElection(String electionId) {
        List<Election> elections = loadElections();
        boolean removed = elections.removeIf(e -> e.getElectionId().equalsIgnoreCase(electionId));
        if (removed) {
            saveElections(elections);

            // Remove candidates associated with this election
            List<Candidate> candidates = loadCandidates();
            candidates.removeIf(c -> c.getElectionId().equalsIgnoreCase(electionId));
            saveCandidates(candidates);

            // Remove votes associated with this election
            List<Vote> votes = loadVotes();
            votes.removeIf(v -> v.getElectionId().equalsIgnoreCase(electionId));
            saveVotes(votes);

            // Reset voters who voted in this election
            List<Voter> voters = loadVoters();
            for (Voter v : voters) {
                if (electionId.equalsIgnoreCase(v.getVotedElectionId())) {
                    v.setHasVoted(false);
                    v.setVotedElectionId("");
                }
            }
            saveVoters(voters);
        }
        return removed;
    }

    // ==========================================
    // VOTES
    // ==========================================
    public synchronized List<Vote> loadVotes() {
        List<Vote> votes = new ArrayList<>();
        File file = new File(votesFile);
        if (!file.exists())
            return votes;

        int skipped = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 5) {
                    skipped++;
                    warnMalformedRecord("votes.txt", line, "expected 5 columns, found " + parts.length);
                    continue;
                }
                String voteId = parts[0];
                String electionId = parts[1];
                String candidateId = parts[2];
                String voterId = parts[3];
                LocalDateTime timestamp = parseDateTime(parts[4]);
                if (timestamp == null) {
                    skipped++;
                    warnMalformedRecord("votes.txt", line, "unparseable ballot timestamp");
                    continue;
                }
                votes.add(new Vote(voteId, electionId, candidateId, voterId, timestamp));
            }
        } catch (IOException e) {
            System.err.println("Error loading votes: " + e.getMessage());
        }
        if (skipped > 0) {
            System.err.println("Warning: " + skipped + " malformed record(s) skipped in " + votesFile);
        }
        return votes;
    }

    public synchronized boolean saveVotes(List<Vote> votes) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(votesFile))) {
            for (Vote vote : votes) {
                String timeStr = vote.getTimestamp() != null ? vote.getTimestamp().format(DATE_TIME_FORMATTER) : "";
                String line = String.join(DELIMITER,
                        vote.getVoteId(),
                        vote.getElectionId(),
                        vote.getCandidateId(),
                        vote.getVoterId(),
                        timeStr);
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error saving votes: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean addVote(Vote vote) {
        List<Vote> votes = loadVotes();
        votes.add(vote);
        return saveVotes(votes);
    }

    // ==========================================
    // AUDIT LOG DELETION SECURITY (PASSWORD PROTECTION)
    // ==========================================

    /**
     * Computes the SHA-256 cryptographic digest of a password.
     */
    public static String hashSHA256(String input) {
        if (input == null)
            return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in JVM", e);
        }
    }

    /**
     * Checks if an audit protection password has been configured.
     */
    public synchronized boolean hasAuditProtectionPassword() {
        File file = new File(auditSecurityFile);
        if (!file.exists() || file.length() == 0) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("audit.password.hash=")) {
                    String hash = line.substring("audit.password.hash=".length()).trim();
                    return !hash.isEmpty();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading audit security file: " + e.getMessage());
        }
        return false;
    }

    /**
     * Stores the SHA-256 hash of the audit protection password to disk.
     */
    public synchronized boolean setAuditProtectionPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            return false;
        }
        String hash = hashSHA256(plainPassword.trim());
        File file = new File(auditSecurityFile);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write("# VOTUM Security Configuration — Protected Audit Trail");
            writer.newLine();
            writer.write("audit.password.hash=" + hash);
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Error saving audit protection password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies an entered plain password against the stored SHA-256 hash.
     */
    public synchronized boolean verifyAuditProtectionPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            return false;
        }
        File file = new File(auditSecurityFile);
        if (!file.exists()) {
            return false;
        }
        String storedHash = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("audit.password.hash=")) {
                    storedHash = line.substring("audit.password.hash=".length()).trim();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error verifying audit protection password: " + e.getMessage());
            return false;
        }
        if (storedHash.isEmpty()) {
            return false;
        }
        String inputHash = hashSHA256(plainPassword.trim());
        return storedHash.equalsIgnoreCase(inputHash);
    }

    /**
     * Clears the stored audit protection password (used for test resets).
     */
    public synchronized boolean clearAuditProtectionPassword() {
        File file = new File(auditSecurityFile);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    /**
     * Reports a malformed persisted record without aborting the rest of the file.
     */
    private static void warnMalformedRecord(String fileName, String record, String reason) {
        System.err.println("Warning: skipping malformed record in " + fileName + " (" + reason + "): " + record);
    }

    /**
     * Removes the field delimiter and line breaks from a value before it is
     * written.
     * Without this guard, a name or slogan containing ';' would shift every
     * following
     * column of the record and silently corrupt the stored ballot/election data.
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(DELIMITER, ",").replace("\r", " ").replace("\n", " ");
    }

    /**
     * Parses an election status token defensively (returns null instead of
     * throwing).
     */
    private static ElectionStatus parseStatus(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return ElectionStatus.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            assert e != null;
            return null;
        }
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            assert e != null;
            try {
                return LocalDateTime.parse(text);
            } catch (Exception ignored) {
                assert ignored != null;
                return null;
            }
        }
    }
}
