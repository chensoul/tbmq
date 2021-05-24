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
package org.thingsboard.mqtt.broker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.mqtt.broker.common.data.User;
import org.thingsboard.mqtt.broker.common.data.exception.ThingsboardException;
import org.thingsboard.mqtt.broker.common.data.page.PageData;
import org.thingsboard.mqtt.broker.common.data.page.PageLink;
import org.thingsboard.mqtt.broker.dao.user.UserService;
import org.thingsboard.mqtt.broker.dto.AdminDto;
import org.thingsboard.mqtt.broker.service.user.AdminService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    public User saveAdmin(@RequestBody AdminDto adminDto) throws ThingsboardException {
        try {
            return adminService.createAdmin(adminDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteAdmin(@PathVariable("userId") String strUserId) throws ThingsboardException {
        checkParameter("userId", strUserId);
        try {
            UUID userId = toUUID(strUserId);
            checkUserId(userId);
            userService.deleteUser(userId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getAdmins(@RequestParam int pageSize, @RequestParam int page) throws ThingsboardException {
        try {
            PageLink pageLink = new PageLink(pageSize, page);
            return checkNotNull(userService.findUsers(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}