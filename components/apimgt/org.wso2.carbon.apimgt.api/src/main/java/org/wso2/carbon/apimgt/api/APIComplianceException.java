/*
 *
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.carbon.apimgt.api;

public class APIComplianceException extends APIManagementException {
    private ErrorHandler errorHandler;


    public APIComplianceException(String message) {
        super(message, ExceptionCodes.from(ExceptionCodes.COMPLIANCE_VIOLATION_ERROR, message));
    }

    public APIComplianceException(String message, ExceptionCodes code) {
        super(message, ExceptionCodes.from(code, message));
    }

    public APIComplianceException(String message, Throwable e) {
        super(message, e);
    }

    public APIComplianceException(Throwable throwable) {
        super(throwable);
    }

}
