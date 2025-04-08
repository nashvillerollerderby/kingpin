import { createSlice } from '@reduxjs/toolkit'
import type { PayloadAction } from '@reduxjs/toolkit'

// export class User {
//   isPlayer?: boolean;
//
//   constructor(public username?: string) {}
//
//   static fromJson(jsonString: string): User | undefined {
//     try {
//       const parsed = JSON.parse(jsonString);
//       const user = new User(parsed.username);
//       if (parsed.leagues) {
//
//       }
//       if (parsed.teams) {
//
//       }
//       if (parsed.tournaments) {
//
//       }
//       if (parsed.bouts) {
//
//       }
//       return user;
//     } catch (e) {
//       console.warn(e);
//     }
//     return undefined;
//   }
// }

export interface User {
  isPlayer?: boolean;
  username: string;
}

export interface UserState {
  isLoggedIn: boolean
  user?: {}
}

const USER_TOKEN_KEY = 'kingpin-user-token';
const token = localStorage.getItem(USER_TOKEN_KEY);
let initialLoggedIn = false;
let initialUser: User | undefined = undefined;
if (token) {
  // validate
  const { username } = JSON.parse(token);
  initialUser = {
    username
  };
  initialLoggedIn = !!initialUser;
}

const initialState: UserState = {
  isLoggedIn: initialLoggedIn,
  user: initialUser
}

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser: (state, _action: PayloadAction<string>) => {
      state.isLoggedIn = true;
      // state.user = new User(action.payload);
      // localStorage.setItem(USER_TOKEN_KEY, JSON.stringify({ username: state.user.username }));
    },
    logout: state => {
      state.isLoggedIn = false;
      state.user = undefined;
    }
  }
})

export const { setUser, logout } = userSlice.actions

export default userSlice.reducer