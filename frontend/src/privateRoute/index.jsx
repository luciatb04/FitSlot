import React, { useEffect, useState } from 'react';
import tokenService from '../services/token.service';
import Login from '../auth/login';

const PrivateRoute = ({ children }) => {
    const jwt = tokenService.getLocalAccessToken();
    const [isLoading, setIsLoading] = useState(true);
    const [isValid, setIsValid] = useState(null);
    const [message, setMessage] = useState(null);
    useEffect(() => {
        if (!jwt) {
            setIsLoading(false);
            return;
        }

        fetch(`/api/v1/auth/validate?token=${encodeURIComponent(jwt)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Token validation failed (${response.status})`);
                }
                return response.json();
            })
            .then(valid => {
                setIsValid(valid);
                if (!valid) {
                    setMessage("Your token has expired. Please, sign in again.");
                }
            })
            .catch(() => {
                setMessage("Unable to validate your session. Please, sign in again.");
                setIsValid(false);
            })
            .finally(() => setIsLoading(false));
    }, [jwt]);

    if (!jwt) return <Login message={message} navigation={false} />;

    if (isLoading === true) {
        return <div>Loading...</div>;
    } else return isValid === true ? children : <Login message={message} navigation={true} />
};

export default PrivateRoute;