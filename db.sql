USE javaproject;

-- Drop tables in correct order (reverse dependency order)
DROP TABLE IF EXISTS server_invites;
DROP TABLE IF EXISTS server_members;
DROP TABLE IF EXISTS servers;
DROP TABLE IF EXISTS blocks;
DROP TABLE IF EXISTS friends;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS users;

-- Users table (no foreign keys)
CREATE TABLE IF NOT EXISTS `users` (
  `userID` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `PW_Hash` binary(60) NOT NULL,
  `token` varchar(64) NOT NULL,
  PRIMARY KEY (`userID`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Messages table (ON DELETE SET NULL - keep messages, remove username)
CREATE TABLE IF NOT EXISTS `messages` (
  `messageID` int(11) NOT NULL AUTO_INCREMENT,
  `message` varchar(255) NOT NULL,
  `sentOn` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `userID` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`messageID`),
  KEY `messages_users_fk` (`userID`),
  CONSTRAINT `messages_users_fk` FOREIGN KEY (`userID`) REFERENCES `users` (`userID`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Friends table (ON DELETE CASCADE - delete friend records when user deleted)
CREATE TABLE IF NOT EXISTS `friends` (
  `requesterID` int(11) NOT NULL,
  `addresseeID` int(11) NOT NULL,
  `status` ENUM('PENDING','ACCEPTED') NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `accepted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`requesterID`, `addresseeID`),
  CONSTRAINT `fk_friends_requester` FOREIGN KEY (`requesterID`) REFERENCES `users` (`userID`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_friends_addressee` FOREIGN KEY (`addresseeID`) REFERENCES `users` (`userID`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `chk_no_self_friend` CHECK (`requesterID` <> `addresseeID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Blocks table (ON DELETE CASCADE - delete block records when user deleted)

CREATE TABLE IF NOT EXISTS blocks (
    userID INT NOT NULL,
    blockedID INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (userID, blockedID),
    CONSTRAINT fk_blocks_user
        FOREIGN KEY (userID) REFERENCES users(userID)
        ON DELETE CASCADE
        ON UPDATE NO ACTION,
    CONSTRAINT fk_blocks_blocked
        FOREIGN KEY (blockedID) REFERENCES users(userID)
        ON DELETE CASCADE
        ON UPDATE NO ACTION,
    CONSTRAINT chk_no_self_block
        CHECK (userID <> blockedID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Servers table: supports public/private, owner is admin (references users.userID)
CREATE TABLE IF NOT EXISTS servers (
  serverID INT(11) NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  ownerID INT(11) NULL,
  is_public BOOLEAN NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (serverID),
  KEY servers_owner_fk (ownerID),
  CONSTRAINT servers_owner_fk
    FOREIGN KEY (ownerID) REFERENCES users(userID) 
    ON DELETE SET NULL
    ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Server members: tracks user membership, role, and permissions
CREATE TABLE IF NOT EXISTS server_members (
  userID INT NOT NULL,
  serverID INT NOT NULL,
  can_invite BOOLEAN NOT NULL DEFAULT FALSE,
  can_kick BOOLEAN NOT NULL DEFAULT FALSE,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (userID, serverID),
  KEY sm_user_fk (userID),
  KEY sm_server_fk (serverID),
  CONSTRAINT sm_user_fk
    FOREIGN KEY (userID) REFERENCES users(userID) 
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT sm_server_fk
    FOREIGN KEY (serverID) REFERENCES servers(serverID) 
    ON DELETE CASCADE
    ON UPDATE NO ACTION
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Server invites: tracks pending invites for private servers
CREATE TABLE IF NOT EXISTS server_invites (
  inviteID INT (11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  serverID INT NOT NULL,
  invitedID INT NOT NULL,
  invited_by INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (inviteID),
  UNIQUE KEY unique_invite (serverID, invitedID),
  KEY si_server_fk (serverID),
  KEY si_invited_fk (invitedID),
  KEY si_inviter_fk (invited_by),
  CONSTRAINT si_server_fk
    FOREIGN KEY (serverID)
    REFERENCES servers(serverID)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT si_invited_fk
    FOREIGN KEY (invitedID)
    REFERENCES users(userID)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT si_inviter_fk
    FOREIGN KEY (invited_by)
    REFERENCES users(userID)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

DROP TRIGGER IF EXISTS trg_blocks_after_insert_remove_friendship;

DELIMITER $$

CREATE TRIGGER trg_blocks_after_insert_remove_friendship
AFTER INSERT ON blocks
FOR EACH ROW
BEGIN
    DELETE FROM friends
    WHERE (requesterID = NEW.userID AND addresseeID = NEW.blockedID)
       OR (requesterID = NEW.blockedID AND addresseeID = NEW.userID);
END$$

DELIMITER ;