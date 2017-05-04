#!/usr/bin/env bash

thesaurus=""
conceptUri=""
auth=""

curl --user "$auth" "http://bde.poolparty.biz/PoolParty/api/thesaurus/${thesaurus}/concept?concept=${conceptUri}&properties=all