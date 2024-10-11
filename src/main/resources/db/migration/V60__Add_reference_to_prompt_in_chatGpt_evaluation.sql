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

-- Add reference to prompt in chatGpt_evaluation nullable
ALTER TABLE chat_gpt_evaluation ADD COLUMN prompt_id BIGINT;
-- Add foreign key constraint linking chat_gpt_evaluation.prompt_id to chat_gpt_prompt.id
ALTER TABLE chat_gpt_evaluation ADD CONSTRAINT fk_chat_gpt_evaluation_prompt_id FOREIGN KEY (prompt_id) REFERENCES chat_gpt_prompt(id);
