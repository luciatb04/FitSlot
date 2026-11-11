import axios from "axios";
import TokenService from "./token.service";

const instance = axios.create({
    baseURL: "/api/v1",
    headers: {
        "Content-Type": "application/json",
    },
});

instance.interceptors.request.use(
    (config) => {
        const token = TokenService.getLocalAccessToken();
        if (token) {
            config.headers["Authorization"] = 'Bearer ' + token;  // for Spring Boot back-end
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

instance.interceptors.response.use(
    (res) => {
        return res;
    },
    async (err) => {
        const originalConfig = err.config;

        if (
            originalConfig &&
            originalConfig.url !== "/auth/signin" &&
            originalConfig.url !== "/auth/refreshtoken" &&
            err.response
        ) {
            // Access Token was expired
            if (err.response.status === 401 && !originalConfig._retry) {
                originalConfig._retry = true;
                const refreshToken = TokenService.getLocalRefreshToken();

                if (!refreshToken) {
                    TokenService.removeUser();
                    return Promise.reject(err);
                }

                try {
                    const rs = await instance.post("/auth/refreshtoken", {
                        refreshToken,
                    });

                    const { accessToken } = rs.data;
                    TokenService.updateLocalAccessToken(accessToken);

                    return instance(originalConfig);
                } catch (refreshError) {
                    TokenService.removeUser();
                    return Promise.reject(refreshError);
                }
            }
        }

        return Promise.reject(err);
    }
);

export default instance;