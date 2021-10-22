import {
    Alert,
    Button,
    Checkbox,
    Dropdown,
    ErrorMessage,
    Form,
    FormGroup,
    Label,
    TextInput,
} from "@trussworks/react-uswds";
import React, { useState } from "react";
import { Link } from "react-router-dom";

import Title from "../../components/Title";
import AuthResource from "../../resources/AuthResource";
import { getStates } from "../../utils/OrganizationUtils";

export interface AgreementBody {
    title: string;
    firstName: string;
    lastName: string;
    email: string;
    territory: string;
    organizationName: string;
    operatesInMultipleStates: boolean;
    agreedToTermsOfService: boolean;
}

function SigningForm({
    signedCallback,
}: {
    signedCallback: (data: AgreementBody) => void;
}) {
    const STATES = getStates()
    /* Form field values are stored here */
    const [title, setTitle] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [territory, setTerritory] = useState(STATES[0].toLowerCase());
    const [organizationName, setOrganizationName] = useState("");
    const [multipleStates, setMultipleStates] = useState(false);
    const [agree, setAgree] = useState(false);

    /* The proper flags are set to true if form is submitted without required fields */
    const [firstNameErrorFlag, setFirstNameErrorFlag] = useState(false);
    const [lastNameErrorFlag, setLastNameErrorFlag] = useState(false);
    const [emailErrorFlag, setemailErrorFlag] = useState(false);
    const [territoryErrorFlag, setterritoryErrorFlag] = useState(false);
    const [organizationNameErrorFlag, setorganizationNameErrorFlag] =
        useState(false);
    const [agreeErrorFlag, setAgreeErrorFlag] = useState(false);
    const [sendGridErrorFlag, setSendGridErrorFlag] = useState({
        isError: false,
        status: 200,
    });

    const handleSubmit = async (e: any) => {
        e.preventDefault();
        resetAllErrorFlags();
        const body = createBody(
            title,
            firstName,
            lastName,
            email,
            territory,
            organizationName,
            multipleStates,
            agree
        );
        if (body) {
            const response = await fetch(
                `${AuthResource.getBaseUrl()}/api/email-registered`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify(body),
                }
            );
            if (response.status >= 200 && response.status <= 299) {
                signedCallback(body);
            } else {
                setSendGridErrorFlag({
                    isError: true,
                    status: response.status,
                });
            }
        }
    };

    /* INFO
       handles the front-end validation and builds the body object of type AgreementBody
       then returns it if no required values are absent. Otherwise, it returns null. */
    const createBody = (
        title: string,
        firstName: string,
        lastName: string,
        email: string,
        territory: string,
        organizationName: string,
        operatesInMultipleStates: boolean,
        agreedToTermsOfService: boolean
    ) => {
        let goodToGo: boolean = true;
        const required: string[] = [
            "firstName",
            "lastName",
            "email",
            "territory",
            "organizationName",
            "agreedToTermsOfService",
        ];
        const body: AgreementBody = {
            title: title,
            firstName: firstName,
            lastName: lastName,
            email: email,
            territory: territory,
            organizationName: organizationName,
            operatesInMultipleStates: operatesInMultipleStates,
            agreedToTermsOfService: agreedToTermsOfService,
        };
        Object.entries(body).forEach((item) => {
            const [key, value]: [string, string | boolean] = item;
            if (
                required.includes(key) &&
                (String(value).trim() === "" || value === false)
            ) {
                goodToGo = false;
                setErrorFlag(key);
            }
        });
        if (goodToGo) {
            return body;
        }
        return null;
    };

    /* INFO
       When resubmitting, this will be called to eliminate all the previous flags
       prior to re-flagging the ones still throwing errors. */
    const resetAllErrorFlags = () => {
        setFirstNameErrorFlag(false);
        setLastNameErrorFlag(false);
        setemailErrorFlag(false);
        setterritoryErrorFlag(false);
        setorganizationNameErrorFlag(false);
        setAgreeErrorFlag(false);
    };

    /* INFO
       Here is where we set flags */
    const setErrorFlag = (key: string) => {
        switch (key) {
            case "firstName":
                setFirstNameErrorFlag(true);
                break;
            case "lastName":
                setLastNameErrorFlag(true);
                break;
            case "email":
                setemailErrorFlag(true);
                break;
            case "territory":
                setterritoryErrorFlag(true);
                break;
            case "organizationName":
                setorganizationNameErrorFlag(true);
                break;
            case "agreedToTermsOfService":
                setAgreeErrorFlag(true);
                break;
            default:
                break;
        }
    };

    const Required = () => {
        return <span style={{ color: "red" }}>*</span>;
    };

    const AgreementLabel = () => {
        return (
            <span className="maxw-2">
                By submitting your information, you're agreeing to the
                ReportSteam <Link to="/terms-of-service">terms of service</Link>
                . <Required />
            </span>
        );
    };

    const ErrorMessageWithFlag = ({ message, flag }: { message: string, flag: boolean }) => {
        if (flag) {
            return (
                <ErrorMessage>
                    <span style={{
                        color: "red",
                    }}>
                        {message}
                    </span>
                </ErrorMessage>
            )
        } else {
            return null;
        }
    }

    return (
        <div data-testid="form-container" className="width-tablet margin-x-auto">
            <Title
                title="Register your organization with ReportStream"
                preTitle="Account registration"
            />
            <p className="usa-prose padding-bottom-9 border-bottom-05 border-base-lighter">
                Required fields are marked with an asterisk (
                <span style={{ color: "red" }}>*</span>).
            </p>

            <Form id="tos-agreement" onSubmit={handleSubmit}>
                <h3 className="padding-top-7 text-normal">
                    Name and contact information
                </h3>
                <FormGroup>
                    <Label htmlFor="title">Title</Label>
                    <TextInput
                        id="title"
                        name="title"
                        type="text"
                        inputSize="small"
                        value={title}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setTitle(e.target.value)
                        }
                    />
                </FormGroup>
                <FormGroup error={firstNameErrorFlag}>
                    <Label htmlFor="first-name">
                        First name <Required />
                    </Label>
                    <TextInput
                        alt="First name input"
                        id="first-name"
                        name="first-name"
                        type="text"
                        value={firstName}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setFirstName(e.target.value)
                        }
                    />
                    <ErrorMessageWithFlag
                        flag={firstNameErrorFlag}
                        message="First name is a required field" />
                </FormGroup>
                <FormGroup error={lastNameErrorFlag}>
                    <Label htmlFor="last-name">
                        Last name <Required />
                    </Label>
                    <TextInput
                        alt="Last name input"
                        id="last-name"
                        name="last-name"
                        type="text"
                        value={lastName}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setLastName(e.target.value)
                        }
                    />

                    <ErrorMessageWithFlag
                        flag={lastNameErrorFlag}
                        message="Last name is a required field" />
                </FormGroup>
                <FormGroup error={emailErrorFlag}>
                    <Label htmlFor="email">
                        Email <Required />
                    </Label>
                    <TextInput
                        alt="Email input"
                        id="email"
                        name="email"
                        type="email"
                        value={email}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setEmail(e.target.value)
                        }
                    />

                    <ErrorMessageWithFlag
                        flag={emailErrorFlag}
                        message="Email is a required field" />
                </FormGroup>

                <h3 className="padding-top-5 text-normal">
                    About your organization
                </h3>
                <FormGroup error={territoryErrorFlag}>
                    <Label htmlFor="states-dropdown">
                        [HQ] State or territory <Required />
                    </Label>
                    <Dropdown
                        id="input-dropdown"
                        name="states-dropdown"
                        value={territory}
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                            setTerritory(e.target.value)
                        }
                    >
                        {STATES.map((state) => {
                            return (
                                <option
                                    key={state.toLowerCase()}
                                    value={state.toLowerCase()}
                                >
                                    {state}
                                </option>
                            );
                        })}
                    </Dropdown>

                    <ErrorMessageWithFlag
                        flag={territoryErrorFlag}
                        message="State or Territory is a required field" />
                </FormGroup>
                <FormGroup error={organizationNameErrorFlag}>
                    <Label htmlFor="organization-name">
                        Organization name <Required />
                    </Label>
                    <TextInput
                        alt="Organization input"
                        id="organization-name"
                        name="organization-name"
                        type="text"
                        value={organizationName}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setOrganizationName(e.target.value)
                        }
                    />

                    <ErrorMessageWithFlag
                        flag={organizationNameErrorFlag}
                        message="Organization is a required field" />
                </FormGroup>
                <Checkbox
                    alt="Multiple states checkbox"
                    className="padding-top-3"
                    id="multi-state"
                    name="multi-state"
                    label="My org reports to multiple states"
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setMultipleStates(e.target.checked)
                    }
                />
            </Form>

            <section className="usa-section usa-prose font-sans-2xs text-base-darker border-top-05 border-base-lighter margin-top-9">
                <p>
                    ReportStream will use the information you’ve provided to
                    communicate with you for the purpose of setting up a
                    connection to the ReportStream platform, and to provide
                    support.
                </p>
                <FormGroup error={agreeErrorFlag}>
                    <Checkbox
                        alt="Agreed checkbox"
                        className="padding-top-3"
                        id="agree"
                        name="agree"
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                            setAgree(e.target.checked)
                        }
                        label={<AgreementLabel />}
                    />

                    <ErrorMessageWithFlag
                        flag={agreeErrorFlag}
                        message="You must agree to the Terms of Service before using ReportStream" />
                </FormGroup>
            </section>

            <Button
                form="tos-agreement"
                className="margin-bottom-10"
                type="submit"
            >
                Submit registration
            </Button>
            <Alert
                style={{
                    visibility: sendGridErrorFlag.isError
                        ? "visible"
                        : "hidden",
                }}
                type="error"
            >
                Oh no! There was an error sending this data. Code:{" "}
                {sendGridErrorFlag.status}
            </Alert>
        </div>
    );
}

export default SigningForm;
