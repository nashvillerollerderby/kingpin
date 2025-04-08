import { Action, configureStore, ThunkAction } from "@reduxjs/toolkit";
import { setupListeners } from "@reduxjs/toolkit/query";
import userReducer from "./userSlice";
import gameReducer from "./gameSlice";
import { ndiApi } from './ndiService';

export const store = configureStore({
  reducer: {
    user: userReducer,
    game: gameReducer,
    [ndiApi.reducerPath]: ndiApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(ndiApi.middleware),
})

export type KingpinStore = typeof store
export type RootState = ReturnType<KingpinStore['getState']>
export type KingpinDispatch = KingpinStore['dispatch']
export type KingpinThunk<ThunkReturnType = void> = ThunkAction<
  ThunkReturnType,
  RootState,
  unknown,
  Action
>
setupListeners(store.dispatch);