@echo off

@REM This script was based on the original conda activate.bat
@REM Also see: https://github.com/gqmelo/exec-wrappers/blob/master/exec_wrappers/templates/conda/run-in.bat
@REM Note that we could activate the env and then print the vars, but activating is slow, so, we just
@REM have the parts which are actually needed for this.


@setlocal

@set "CONDA_PREFIX=%__PYDEV_CONDA_PREFIX__%"
@set "CONDA_DEFAULT_ENV=%__PYDEV_CONDA_DEFAULT_ENV__%"
@set "CONDA_ACTIVATION=%__PYDEV_CONDA_ACTIVATION__%"
@set "CONDA_ENV_PATH=%CONDA_PREFIX%"
@set "CONDA_PATH_BACKUP=%PATH%"
@set "PATH=%CONDA_PREFIX%;%CONDA_PREFIX%\Scripts;%CONDA_PREFIX%\Library\bin;%PATH%"

@REM activate conda and get the environment
@call %CONDA_ACTIVATION%
@call activate %CONDA_DEFAULT_ENV%

@REM Print existing environment variables with the loaded env.
SET

@endlocal