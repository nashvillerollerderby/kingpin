import { useState } from "react";
import { setUser } from "../store/userSlice";
import { useDispatch } from "react-redux";
import { NavLink, useNavigate } from "react-router";

export function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const login = async () => {
    dispatch(setUser(username));
    navigate("/");
  };

  return (
    <>
      <div className="flex flex-col gap-4 items-center p-4">
        <div className="w-xs">
          <h1 className="text-xl">Login</h1>
        </div>
        <input type="text" placeholder="Username" className="input" value={username}
               onChange={(e) => setUsername(e.target.value)}/>
        <input type="password" placeholder="Password" className="input" value={password}
               onChange={(e) => setPassword(e.target.value)}/>
        {/*<p className="fieldset-label">You can edit page title later on from settings</p>*/}
        <button className="btn btn-primary btn-sm" onClick={login}>Submit</button>
        <NavLink to="/new-account" className="link link-hover">Need a new account?</NavLink>
      </div>
    </>
  )
}
