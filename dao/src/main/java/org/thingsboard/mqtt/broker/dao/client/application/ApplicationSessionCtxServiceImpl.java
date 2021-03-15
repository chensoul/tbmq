/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.mqtt.broker.dao.client.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.mqtt.broker.common.data.ApplicationSessionCtx;
import org.thingsboard.mqtt.broker.dao.exception.DataValidationException;

import java.util.Optional;

@Service
@Slf4j
public class ApplicationSessionCtxServiceImpl implements ApplicationSessionCtxService {

    @Autowired
    private ApplicationSessionCtxDao applicationSessionCtxDao;

    @Override
    public ApplicationSessionCtx saveApplicationSessionCtx(ApplicationSessionCtx applicationSessionCtx) {
        log.trace("Executing saveApplicationSessionCtx [{}]", applicationSessionCtx);
        validate(applicationSessionCtx);
        return applicationSessionCtxDao.save(applicationSessionCtx);
    }

    @Override
    public void deleteApplicationSessionCtx(String clientId) {
        log.trace("Executing deleteApplicationSessionCtx [{}]", clientId);
        applicationSessionCtxDao.remove(clientId);
    }

    @Override
    public Optional<ApplicationSessionCtx> findApplicationSessionCtx(String clientId) {
        log.trace("Executing findApplicationSessionCtx [{}]", clientId);
        return Optional.ofNullable(applicationSessionCtxDao.find(clientId));
    }

    private void validate(ApplicationSessionCtx applicationSessionCtx) {
        if (StringUtils.isEmpty(applicationSessionCtx.getClientId())) {
            throw new DataValidationException("Client ID should be specified!");
        }
        if (applicationSessionCtx.getPublishedMsgInfos() == null) {
            throw new DataValidationException("Published messages infos should be specified!");
        }
    }
}