import { PropsWithChildren } from "react";
import { NavLink, useNavigate } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "./store/store";
import { logout } from "./store/userSlice";

function Layout(props: PropsWithChildren) {
  const isLoggedIn = useSelector((state: RootState) => state.user.isLoggedIn);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const dispatchLogout = () => {
    dispatch(logout());
    navigate("/");
  }

  const loggedInNavbar = () => isLoggedIn ? (
    <>
      <NavLink className="btn btn-ghost" to="/leagues">Leagues</NavLink>
      <NavLink className="btn btn-ghost" to="/leagues">Teams</NavLink>
      <NavLink className="btn btn-ghost" to="/leagues">Tournaments</NavLink>
      <NavLink className="btn btn-ghost" to="/leagues">Bouts</NavLink>
      <NavLink className="btn btn-ghost" to="/game">Game</NavLink>
      <NavLink className="btn btn-ghost" to="/stream-controls">Stream Controls</NavLink>
    </>
  ) : null

  return (
    <>
      <div className="navbar bg-base-300 shadow-sm">
        <div className="navbar-start">
          <button className="btn btn-square btn-ghost md:hidden">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
                 className="inline-block h-5 w-5 stroke-current">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
          <NavLink className="btn btn-ghost text-xl p-x-8" to="/">Kingpin</NavLink>
          {loggedInNavbar()}
        </div>
        <div className="navbar-end">
          {isLoggedIn ? <button className="btn btn-ghost" onClick={dispatchLogout}>Logout</button> :
            <NavLink className="btn btn-ghost" to="/login">Login</NavLink>}
        </div>
      </div>
      {props.children}
    </>
  )
}

export default Layout
