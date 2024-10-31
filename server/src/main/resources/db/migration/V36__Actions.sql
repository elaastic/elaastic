#
# Elaastic - formative assessment system
# Copyright (C) 2019. University Toulouse 1 Capitole, University Toulouse 3 Paul Sabatier
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

CREATE TABLE `action` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL,
  `sequence_id` bigint(20) NOT NULL,
  `subject` varchar(32) NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `object` varchar(32),
  PRIMARY KEY (`id`),
  KEY `idx_action_sequence_id` (`sequence_id`),
  CONSTRAINT `fk_action_sequence` FOREIGN KEY (`sequence_id`) REFERENCES `sequence` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;