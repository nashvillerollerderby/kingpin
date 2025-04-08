// Use throughout your app instead of plain `useDispatch` and `useSelector`
import { useDispatch, useSelector } from "react-redux";
import { KingpinDispatch, RootState } from "./store.ts";

export const useKingpinDispatch = useDispatch.withTypes<KingpinDispatch>()
export const useKingpinSelector = useSelector.withTypes<RootState>()