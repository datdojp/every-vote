class ApiController < ActionController::Base

  def login
    type = params[:type]
    sns_id = params[:sns_id]
    access_token = params[:access_token]

    # TODO: should validate access token with SNS 3rd party server

    query = SnsAccount.where(type: type, sns_id: sns_id)
    if query.exists?
      user = query.first.user
    else
      user = User.create(
        access_token: SecureRandom.hex
      )
      sns_account = SnsAccount.create(
        type: type,
        sns_id: sns_id,
        access_token: access_token,
        user: user
      )
    end

    render_response({id: user.id, access_token: user.access_token}, 0, nil)
  end

  private

  def render_response(data, err_code, err_msg)
    render json: { result: data, err_code: err_code, err_msg => (err_msg or "") }, status: 200
  end
end
