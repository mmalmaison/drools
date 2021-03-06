/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.event.rule.impl;

import org.kie.event.rule.MatchCancelledCause;
import org.kie.event.rule.MatchCancelledEvent;
import org.kie.runtime.KnowledgeRuntime;
import org.kie.runtime.rule.Match;


public class ActivationCancelledEventImpl extends ActivationEventImpl implements MatchCancelledEvent {
    private MatchCancelledCause cause;
    
    public ActivationCancelledEventImpl(Match activation, KnowledgeRuntime kruntime, MatchCancelledCause cause) {
        super( activation, kruntime);
        this.cause = cause;
    }

    public MatchCancelledCause getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "==>[ActivationCancelledEvent: getCause()=" + getCause() + ", getActivation()=" + getMatch()
                + ", getKnowledgeRuntime()=" + getKnowledgeRuntime() + "]";
    }

}
